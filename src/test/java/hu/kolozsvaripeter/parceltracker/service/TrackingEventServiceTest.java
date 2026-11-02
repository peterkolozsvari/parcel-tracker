package hu.kolozsvaripeter.parceltracker.service;

import hu.kolozsvaripeter.parceltracker.domain.*;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import hu.kolozsvaripeter.parceltracker.exception.ShipmentNotFoundException;
import hu.kolozsvaripeter.parceltracker.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingEventServiceTest {
    @Mock UserRepository userRepository;
    @Mock AddressRepository addressRepository;
    @Mock ShipmentRepository shipmentRepository;
    @Mock ParcelRepository parcelRepository;
    @Mock TrackingEventRepository trackingEventRepository;
    @InjectMocks ShipmentService service;

    private void givenAccess(Role role, Long userId) {
        User sender = new User();
        sender.setId(11L);
        Shipment shipment = new Shipment();
        shipment.setId(2L);
        shipment.setSender(sender);
        User viewer = new User();
        viewer.setId(userId);
        viewer.setRole(role);
        when(shipmentRepository.findById(2L)).thenReturn(Optional.of(shipment));
        when(userRepository.findByEmailIgnoreCase("viewer@example.test")).thenReturn(Optional.of(viewer));
    }

    @Test
    void mapsEventsAndPreservesRepositoryOrderForEqualTimes() {
        givenAccess(Role.CUSTOMER, 11L);
        var time = OffsetDateTime.parse("2026-11-02T10:00:00Z");
        TrackingEvent first = mock(TrackingEvent.class);
        when(first.getId()).thenReturn(10L);
        when(first.getStatus()).thenReturn(ShipmentStatus.REGISTERED);
        when(first.getDescription()).thenReturn("Shipment registered");
        when(first.getOccurredAt()).thenReturn(time);
        TrackingEvent second = mock(TrackingEvent.class);
        when(second.getId()).thenReturn(11L);
        when(second.getStatus()).thenReturn(ShipmentStatus.COURIER_ASSIGNED);
        when(second.getOccurredAt()).thenReturn(time);
        when(trackingEventRepository.findAllByShipmentIdOrderByOccurredAtAscIdAsc(2L))
                .thenReturn(List.of(first, second));

        var result = service.getTrackingEvents(2L, "viewer@example.test");

        assertEquals(List.of(10L, 11L), result.stream().map(event -> event.id()).toList());
        assertEquals(ShipmentStatus.REGISTERED, result.getFirst().status());
        assertEquals("Shipment registered", result.getFirst().description());
        assertEquals(time, result.getFirst().occurredAt());
        assertNull(result.get(1).description());
        verify(trackingEventRepository).findAllByShipmentIdOrderByOccurredAtAscIdAsc(2L);
    }

    @Test
    void returnsEmptyHistoryForShipmentWithoutEvents() {
        givenAccess(Role.CUSTOMER, 11L);
        when(trackingEventRepository.findAllByShipmentIdOrderByOccurredAtAscIdAsc(2L)).thenReturn(List.of());
        assertTrue(service.getTrackingEvents(2L, "viewer@example.test").isEmpty());
    }

    @Test
    void adminMayReadHistory() {
        givenAccess(Role.ADMIN, 99L);
        when(trackingEventRepository.findAllByShipmentIdOrderByOccurredAtAscIdAsc(2L)).thenReturn(List.of());
        service.getTrackingEvents(2L, "viewer@example.test");
        verify(trackingEventRepository).findAllByShipmentIdOrderByOccurredAtAscIdAsc(2L);
    }

    @Test
    void deniesOtherCustomerBeforeQueryingEvents() {
        givenAccess(Role.CUSTOMER, 99L);
        assertThrows(AccessDeniedException.class, () -> service.getTrackingEvents(2L, "viewer@example.test"));
        verifyNoInteractions(trackingEventRepository);
    }

    @Test
    void missingShipmentDoesNotQueryEvents() {
        when(shipmentRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ShipmentNotFoundException.class, () -> service.getTrackingEvents(999L, "viewer@example.test"));
        verifyNoInteractions(userRepository, trackingEventRepository);
    }
}
