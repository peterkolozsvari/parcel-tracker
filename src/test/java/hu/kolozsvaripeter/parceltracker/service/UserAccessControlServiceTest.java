package hu.kolozsvaripeter.parceltracker.service;

import hu.kolozsvaripeter.parceltracker.domain.*;
import hu.kolozsvaripeter.parceltracker.dto.incoming.*;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import hu.kolozsvaripeter.parceltracker.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccessControlServiceTest {
    @Mock UserRepository userRepository;
    @Mock AddressRepository addressRepository;
    @Mock ShipmentRepository shipmentRepository;
    @Mock ParcelRepository parcelRepository;
    @Mock TrackingEventRepository trackingEventRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @InjectMocks UserService userService;
    @InjectMocks ShipmentService shipmentService;
    @InjectMocks AuthService authService;

    private User actor(Role role, boolean active) {
        User user = new User(); user.setId(1L); user.setRole(role); user.setActive(active);
        user.setEmail("actor@example.test"); user.setFullName("Teszt Felhasználó");
        when(userRepository.findByEmailIgnoreCase("actor@example.test")).thenReturn(Optional.of(user));
        return user;
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void eachActiveRoleCanReadAndUpdateOwnProfile(Role role) {
        User user = actor(role, true);
        assertEquals(1L, userService.getUserById(1L, "actor@example.test").id());
        userService.updateUser(1L, new UpdateUserProfileDto("Új Név", "123456", null), "actor@example.test");
        assertEquals("Új Név", user.getFullName());
        assertEquals("123456", user.getPhoneNumber());
        assertEquals(role, user.getRole());
        assertEquals("actor@example.test", user.getEmail());
        verify(userRepository).save(user);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void nonAdminCannotReadOrModifyAnotherProfile(Role role) {
        actor(role, true);
        assertThrows(AccessDeniedException.class, () -> userService.getUserById(2L, "actor@example.test"));
        assertThrows(AccessDeniedException.class, () -> userService.updateUser(2L,
                new UpdateUserProfileDto("Tampered", null, null), "actor@example.test"));
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(addressRepository);
    }

    @Test
    void adminCanReadAndUpdateAnotherProfile() {
        actor(Role.ADMIN, true);
        User target = new User(); target.setId(2L); target.setRole(Role.CUSTOMER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        assertEquals(2L, userService.getUserById(2L, "actor@example.test").id());
        userService.updateUser(2L, new UpdateUserProfileDto("Updated", null, null), "actor@example.test");
        assertEquals("Updated", target.getFullName());
        verify(userRepository).save(target);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void inactiveSessionCannotReadOrUpdateProfile(Role role) {
        actor(role, false);
        assertThrows(AccessDeniedException.class, () -> userService.getUserById(1L, "actor@example.test"));
        assertThrows(AccessDeniedException.class, () -> userService.updateUser(1L,
                new UpdateUserProfileDto("Tampered", null, null), "actor@example.test"));
        verify(userRepository, never()).save(any());
        verifyNoInteractions(addressRepository);
    }

    private CreateShipmentDto command(Long senderId) {
        var address = new CreateAddressDto("Magyarország", "1211", "Budapest", "Teszt utca", "1", null);
        return new CreateShipmentDto(senderId, address, "Teszt Címzett", address,
                List.of(new CreateParcelDto(new BigDecimal("1.50"), null, false)));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void cannotCreateShipmentForForgedSenderEvenAsAdmin(Role role) {
        actor(role, true);
        assertThrows(AccessDeniedException.class,
                () -> shipmentService.createShipment(command(2L), "actor@example.test"));
        verifyNoInteractions(addressRepository, shipmentRepository, parcelRepository, trackingEventRepository);
    }

    @Test
    void activeUserCreatesShipmentForSelfAndIsRecordedAsActor() {
        User user = actor(Role.CUSTOMER, true);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0); shipment.setId(9L); return shipment;
        });
        var result = shipmentService.createShipment(command(1L), "actor@example.test");
        assertEquals(9L, result.id());
        var shipment = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(shipment.capture());
        assertSame(user, shipment.getValue().getSender());
        assertEquals(ShipmentStatus.REGISTERED, shipment.getValue().getStatus());
        var event = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(trackingEventRepository).save(event.capture());
        assertSame(user, event.getValue().getChangedByUser());
    }

    @Test
    void inactiveUserCannotCreateEvenForOwnId() {
        actor(Role.CUSTOMER, false);
        assertThrows(AccessDeniedException.class,
                () -> shipmentService.createShipment(command(1L), "actor@example.test"));
        verifyNoInteractions(addressRepository, shipmentRepository, parcelRepository, trackingEventRepository);
    }

    @Test
    void inactiveUserCannotListOwnShipments() {
        actor(Role.CUSTOMER, false);
        assertThrows(AccessDeniedException.class,
                () -> shipmentService.getMyShipments("actor@example.test", null, PageRequest.of(0, 10)));
        verifyNoInteractions(shipmentRepository);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void staleAdminSessionCannotBypassCurrentDatabaseRole(Role role) {
        actor(role, true);
        assertThrows(AccessDeniedException.class,
                () -> shipmentService.getShipments(null, null, PageRequest.of(0, 10), "actor@example.test"));
        verifyNoInteractions(shipmentRepository);
    }

    @Test
    void activeAdminCanListAllShipments() {
        actor(Role.ADMIN, true);
        var page = PageRequest.of(0, 10);
        when(shipmentRepository.findAllByFilters(null, null, page)).thenReturn(Page.empty(page));
        assertTrue(shipmentService.getShipments(null, null, page, "actor@example.test").isEmpty());
    }

    @Test
    void inactiveAdminCannotListAllShipments() {
        actor(Role.ADMIN, false);
        assertThrows(AccessDeniedException.class,
                () -> shipmentService.getShipments(null, null, PageRequest.of(0, 10), "actor@example.test"));
        verifyNoInteractions(shipmentRepository);
    }

    @Test
    void disabledAccountCannotRestoreSessionThroughMeEndpoint() {
        actor(Role.CUSTOMER, false);
        assertThrows(DisabledException.class, () -> authService.getCurrentUser("actor@example.test"));
    }
}
