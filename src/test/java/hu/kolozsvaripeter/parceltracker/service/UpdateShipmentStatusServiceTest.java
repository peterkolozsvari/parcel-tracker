package hu.kolozsvaripeter.parceltracker.service;

import hu.kolozsvaripeter.parceltracker.domain.*;
import hu.kolozsvaripeter.parceltracker.dto.incoming.UpdateShipmentStatusDto;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import hu.kolozsvaripeter.parceltracker.exception.*;
import hu.kolozsvaripeter.parceltracker.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateShipmentStatusServiceTest {
    @Mock UserRepository userRepository;
    @Mock AddressRepository addressRepository;
    @Mock ShipmentRepository shipmentRepository;
    @Mock ParcelRepository parcelRepository;
    @Mock TrackingEventRepository trackingEventRepository;
    @InjectMocks ShipmentService service;

    private User givenActor(Role role, Long id, boolean active) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setActive(active);
        when(userRepository.findByEmailIgnoreCase("actor@example.test")).thenReturn(Optional.of(user));
        return user;
    }

    private Shipment givenShipment(ShipmentStatus status) {
        User courier = new User();
        courier.setId(7L);
        Shipment shipment = new Shipment();
        shipment.setId(2L);
        shipment.setCourier(courier);
        shipment.setStatus(status);
        when(shipmentRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(shipment));
        return shipment;
    }

    static Stream<Arguments> statusTransitions() {
        Set<String> allowed = Set.of(
                "REGISTERED:CANCELLED", "COURIER_ASSIGNED:PICKED_UP", "COURIER_ASSIGNED:CANCELLED",
                "PICKED_UP:IN_TRANSIT", "IN_TRANSIT:DELIVERED", "IN_TRANSIT:DELIVERY_FAILED",
                "DELIVERY_FAILED:IN_TRANSIT");
        return Arrays.stream(ShipmentStatus.values()).flatMap(current ->
                Arrays.stream(ShipmentStatus.values()).map(next ->
                        Arguments.of(current, next, allowed.contains(current + ":" + next))));
    }

    @ParameterizedTest(name = "{0} -> {1}: allowed={2}")
    @MethodSource("statusTransitions")
    void validatesEveryStatusTransition(ShipmentStatus current, ShipmentStatus next, boolean allowed) {
        User actor = givenActor(Role.ADMIN, 1L, true);
        Shipment shipment = givenShipment(current);
        var command = new UpdateShipmentStatusDto(next, null);
        if (allowed) {
            service.updateShipmentStatus(2L, command, "actor@example.test");
            assertEquals(next, shipment.getStatus());
            verify(shipmentRepository).save(shipment);
            var event = ArgumentCaptor.forClass(TrackingEvent.class);
            verify(trackingEventRepository).save(event.capture());
            assertSame(shipment, event.getValue().getShipment());
            assertSame(actor, event.getValue().getChangedByUser());
            assertEquals(next, event.getValue().getStatus());
            assertFalse(event.getValue().getDescription().isBlank());
        } else {
            assertThrows(ShipmentStateConflictException.class,
                    () -> service.updateShipmentStatus(2L, command, "actor@example.test"));
            assertEquals(current, shipment.getStatus());
            verify(shipmentRepository, never()).save(any());
            verifyNoInteractions(trackingEventRepository);
        }
    }

    @Test
    void assignedCourierCanPickUpShipmentAndSaveTrimmedDescription() {
        User courier = givenActor(Role.COURIER, 7L, true);
        givenShipment(ShipmentStatus.COURIER_ASSIGNED);
        service.updateShipmentStatus(2L, new UpdateShipmentStatusDto(ShipmentStatus.PICKED_UP, "  Átvéve a feladótól.  "), "actor@example.test");
        var event = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(trackingEventRepository).save(event.capture());
        assertSame(courier, event.getValue().getChangedByUser());
        assertEquals("Átvéve a feladótól.", event.getValue().getDescription());
    }

    @Test
    void otherCourierCannotChangeShipment() {
        givenActor(Role.COURIER, 8L, true);
        givenShipment(ShipmentStatus.COURIER_ASSIGNED);
        assertThrows(AccessDeniedException.class, () -> service.updateShipmentStatus(2L,
                new UpdateShipmentStatusDto(ShipmentStatus.PICKED_UP, null), "actor@example.test"));
        verify(shipmentRepository, never()).save(any());
        verifyNoInteractions(trackingEventRepository);
    }

    @Test
    void assignedCourierCannotCancelShipment() {
        givenActor(Role.COURIER, 7L, true);
        givenShipment(ShipmentStatus.COURIER_ASSIGNED);
        assertThrows(AccessDeniedException.class, () -> service.updateShipmentStatus(2L,
                new UpdateShipmentStatusDto(ShipmentStatus.CANCELLED, null), "actor@example.test"));
        verifyNoInteractions(trackingEventRepository);
    }

    @Test
    void customerCannotChangeShipmentEvenThroughDirectServiceCall() {
        givenActor(Role.CUSTOMER, 11L, true);
        assertThrows(AccessDeniedException.class, () -> service.updateShipmentStatus(2L,
                new UpdateShipmentStatusDto(ShipmentStatus.PICKED_UP, null), "actor@example.test"));
        verifyNoInteractions(shipmentRepository, trackingEventRepository);
    }

    @Test
    void inactiveCourierCannotChangeShipment() {
        givenActor(Role.COURIER, 7L, false);
        assertThrows(AccessDeniedException.class, () -> service.updateShipmentStatus(2L,
                new UpdateShipmentStatusDto(ShipmentStatus.PICKED_UP, null), "actor@example.test"));
        verifyNoInteractions(shipmentRepository, trackingEventRepository);
    }

    @Test
    void courierCannotChangeUnassignedShipment() {
        givenActor(Role.COURIER, 7L, true);
        givenShipment(ShipmentStatus.COURIER_ASSIGNED).setCourier(null);
        assertThrows(AccessDeniedException.class, () -> service.updateShipmentStatus(2L,
                new UpdateShipmentStatusDto(ShipmentStatus.PICKED_UP, null), "actor@example.test"));
        verifyNoInteractions(trackingEventRepository);
    }

    @Test
    void adminCannotAdvanceShipmentWithoutCourier() {
        givenActor(Role.ADMIN, 1L, true);
        givenShipment(ShipmentStatus.COURIER_ASSIGNED).setCourier(null);
        assertThrows(ShipmentStateConflictException.class, () -> service.updateShipmentStatus(2L,
                new UpdateShipmentStatusDto(ShipmentStatus.PICKED_UP, null), "actor@example.test"));
        verifyNoInteractions(trackingEventRepository);
    }

    @Test
    void adminCanCancelUnassignedRegisteredShipment() {
        givenActor(Role.ADMIN, 1L, true);
        var shipment = givenShipment(ShipmentStatus.REGISTERED);
        shipment.setCourier(null);
        service.updateShipmentStatus(2L, new UpdateShipmentStatusDto(ShipmentStatus.CANCELLED, null), "actor@example.test");
        assertEquals(ShipmentStatus.CANCELLED, shipment.getStatus());
    }

    @Test
    void repeatedRequestDoesNotCreateDuplicateEvents() {
        givenActor(Role.COURIER, 7L, true);
        givenShipment(ShipmentStatus.COURIER_ASSIGNED);
        var command = new UpdateShipmentStatusDto(ShipmentStatus.PICKED_UP, " ");
        service.updateShipmentStatus(2L, command, "actor@example.test");
        assertThrows(ShipmentStateConflictException.class,
                () -> service.updateShipmentStatus(2L, command, "actor@example.test"));
        verify(shipmentRepository, times(1)).save(any());
        var event = ArgumentCaptor.forClass(TrackingEvent.class);
        verify(trackingEventRepository, times(1)).save(event.capture());
        assertEquals("A futár felvette a küldeményt.", event.getValue().getDescription());
    }

    @Test
    void missingShipmentReturnsNotFoundWithoutWritingEvent() {
        givenActor(Role.ADMIN, 1L, true);
        when(shipmentRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());
        assertThrows(ShipmentNotFoundException.class, () -> service.updateShipmentStatus(2L,
                new UpdateShipmentStatusDto(ShipmentStatus.PICKED_UP, null), "actor@example.test"));
        verifyNoInteractions(trackingEventRepository);
    }
}
