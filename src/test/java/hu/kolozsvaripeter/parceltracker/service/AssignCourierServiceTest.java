package hu.kolozsvaripeter.parceltracker.service;

import hu.kolozsvaripeter.parceltracker.domain.*;
import hu.kolozsvaripeter.parceltracker.dto.incoming.AssignCourierDto;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import hu.kolozsvaripeter.parceltracker.exception.*;
import hu.kolozsvaripeter.parceltracker.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignCourierServiceTest {
    @Mock UserRepository userRepository;
    @Mock AddressRepository addressRepository;
    @Mock ShipmentRepository shipmentRepository;
    @Mock ParcelRepository parcelRepository;
    @Mock TrackingEventRepository trackingEventRepository;
    @InjectMocks ShipmentService service;

    private User givenAdmin(Role role, boolean active) {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(role);
        admin.setActive(active);
        when(userRepository.findByEmailIgnoreCase("admin@example.test")).thenReturn(Optional.of(admin));
        return admin;
    }

    private Shipment givenShipment(ShipmentStatus status) {
        Shipment shipment = new Shipment();
        shipment.setId(2L);
        shipment.setStatus(status);
        when(shipmentRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(shipment));
        return shipment;
    }

    private User givenCourier(Role role, boolean active) {
        User courier = new User();
        courier.setId(7L);
        courier.setRole(role);
        courier.setActive(active);
        when(userRepository.findById(7L)).thenReturn(Optional.of(courier));
        return courier;
    }

    @Test
    void assignsCourierChangesStatusAndRecordsAdminAsActor() {
        User admin = givenAdmin(Role.ADMIN, true);
        Shipment shipment = givenShipment(ShipmentStatus.REGISTERED);
        User courier = givenCourier(Role.COURIER, true);

        service.assignCourier(2L, new AssignCourierDto(7L), "admin@example.test");

        assertSame(courier, shipment.getCourier());
        assertEquals(ShipmentStatus.COURIER_ASSIGNED, shipment.getStatus());
        verify(shipmentRepository).save(shipment);
        var event = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(trackingEventRepository).save(event.capture());
        assertSame(shipment, event.getValue().getShipment());
        assertSame(admin, event.getValue().getChangedByUser());
        assertEquals(ShipmentStatus.COURIER_ASSIGNED, event.getValue().getStatus());
        assertEquals("Futár hozzárendelve a küldeményhez.", event.getValue().getDescription());
    }

    @Test
    void customerCannotAssignCourierEvenWhenCallingServiceDirectly() {
        givenAdmin(Role.CUSTOMER, true);
        assertThrows(AccessDeniedException.class,
                () -> service.assignCourier(2L, new AssignCourierDto(7L), "admin@example.test"));
        verifyNoInteractions(shipmentRepository, trackingEventRepository);
    }

    @Test
    void inactiveAdminCannotAssignCourier() {
        givenAdmin(Role.ADMIN, false);
        assertThrows(AccessDeniedException.class,
                () -> service.assignCourier(2L, new AssignCourierDto(7L), "admin@example.test"));
        verifyNoInteractions(shipmentRepository, trackingEventRepository);
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, names = "REGISTERED", mode = EnumSource.Mode.EXCLUDE)
    void rejectsEveryNonRegisteredStatus(ShipmentStatus status) {
        givenAdmin(Role.ADMIN, true);
        Shipment shipment = givenShipment(status);
        assertThrows(ShipmentStateConflictException.class,
                () -> service.assignCourier(2L, new AssignCourierDto(7L), "admin@example.test"));
        assertNull(shipment.getCourier());
        assertEquals(status, shipment.getStatus());
        verify(shipmentRepository, never()).save(any());
        verifyNoInteractions(trackingEventRepository);
    }

    @Test
    void rejectsExistingCourierEvenWithInconsistentRegisteredStatus() {
        givenAdmin(Role.ADMIN, true);
        Shipment shipment = givenShipment(ShipmentStatus.REGISTERED);
        shipment.setCourier(new User());
        assertThrows(ShipmentStateConflictException.class,
                () -> service.assignCourier(2L, new AssignCourierDto(7L), "admin@example.test"));
        verifyNoInteractions(trackingEventRepository);
    }

    @Test
    void rejectsInactiveCourierWithoutChangingShipment() {
        givenAdmin(Role.ADMIN, true);
        Shipment shipment = givenShipment(ShipmentStatus.REGISTERED);
        givenCourier(Role.COURIER, false);
        assertThrows(IllegalArgumentException.class,
                () -> service.assignCourier(2L, new AssignCourierDto(7L), "admin@example.test"));
        assertNull(shipment.getCourier());
        assertEquals(ShipmentStatus.REGISTERED, shipment.getStatus());
        verifyNoInteractions(trackingEventRepository);
    }

    @Test
    void rejectsCustomerAsSelectedCourier() {
        givenAdmin(Role.ADMIN, true);
        givenShipment(ShipmentStatus.REGISTERED);
        givenCourier(Role.CUSTOMER, true);
        assertThrows(IllegalArgumentException.class,
                () -> service.assignCourier(2L, new AssignCourierDto(7L), "admin@example.test"));
        verify(shipmentRepository, never()).save(any());
        verifyNoInteractions(trackingEventRepository);
    }

    @Test
    void missingShipmentDoesNotWriteAnything() {
        givenAdmin(Role.ADMIN, true);
        when(shipmentRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());
        assertThrows(ShipmentNotFoundException.class,
                () -> service.assignCourier(2L, new AssignCourierDto(7L), "admin@example.test"));
        verifyNoInteractions(trackingEventRepository);
    }

    @Test
    void missingCourierDoesNotWriteAnything() {
        givenAdmin(Role.ADMIN, true);
        givenShipment(ShipmentStatus.REGISTERED);
        when(userRepository.findById(7L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,
                () -> service.assignCourier(2L, new AssignCourierDto(7L), "admin@example.test"));
        verify(shipmentRepository, never()).save(any());
        verifyNoInteractions(trackingEventRepository);
    }
}
