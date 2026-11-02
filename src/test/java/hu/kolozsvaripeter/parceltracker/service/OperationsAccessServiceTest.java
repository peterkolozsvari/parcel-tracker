package hu.kolozsvaripeter.parceltracker.service;

import hu.kolozsvaripeter.parceltracker.domain.*;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import hu.kolozsvaripeter.parceltracker.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationsAccessServiceTest {
    @Mock UserRepository users;
    @Mock AddressRepository addresses;
    @Mock ShipmentRepository shipments;
    @Mock ParcelRepository parcels;
    @Mock TrackingEventRepository events;
    ShipmentService shipmentService;
    UserService userService;

    @BeforeEach
    void setUp() {
        shipmentService = new ShipmentService(users, addresses, shipments, parcels, events);
        userService = new UserService(users, mock(PasswordEncoder.class), addresses);
    }

    private User viewer(Role role, boolean active) {
        User user = new User();
        user.setId(7L); user.setRole(role); user.setActive(active); user.setFullName("Teszt Futár");
        when(users.findByEmailIgnoreCase("viewer@example.test")).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void courierListUsesAuthenticatedIdAndPreservesFilterAndPage() {
        viewer(Role.COURIER, true);
        var page = PageRequest.of(1, 10);
        when(shipments.findAssignedToCourier(7L, ShipmentStatus.IN_TRANSIT, page)).thenReturn(Page.empty(page));
        assertTrue(shipmentService.getAssignedShipments("viewer@example.test", ShipmentStatus.IN_TRANSIT, page).isEmpty());
        verify(shipments).findAssignedToCourier(7L, ShipmentStatus.IN_TRANSIT, page);
    }

    @Test
    void customerCannotCallAssignedListDirectly() {
        viewer(Role.CUSTOMER, true);
        assertThrows(AccessDeniedException.class,
                () -> shipmentService.getAssignedShipments("viewer@example.test", null, PageRequest.of(0, 10)));
        verifyNoInteractions(shipments);
    }

    @Test
    void inactiveCourierCannotListAssignedShipments() {
        viewer(Role.COURIER, false);
        assertThrows(AccessDeniedException.class,
                () -> shipmentService.getAssignedShipments("viewer@example.test", null, PageRequest.of(0, 10)));
        verifyNoInteractions(shipments);
    }

    @Test
    void adminLoadsOnlyActiveCouriersAndReceivesMinimalData() {
        viewer(Role.ADMIN, true);
        User courier = new User(); courier.setId(8L); courier.setFullName("Futár Béla");
        when(users.findAllByRoleAndActiveTrueOrderByFullNameAscIdAsc(Role.COURIER)).thenReturn(List.of(courier));
        var result = userService.getActiveCouriers("viewer@example.test");
        assertEquals(8L, result.getFirst().id());
        assertEquals("Futár Béla", result.getFirst().fullName());
    }

    @Test
    void courierCannotReadAdminCourierDirectory() {
        viewer(Role.COURIER, true);
        assertThrows(AccessDeniedException.class, () -> userService.getActiveCouriers("viewer@example.test"));
        verify(users, never()).findAllByRoleAndActiveTrueOrderByFullNameAscIdAsc(any());
    }

    private Shipment assignedShipment(User courier) {
        User sender = new User(); sender.setId(99L); sender.setRole(Role.CUSTOMER);
        Shipment shipment = new Shipment(); shipment.setId(2L); shipment.setSender(sender);
        shipment.setCourier(courier); shipment.setPickupAddress(new Address()); shipment.setDeliveryAddress(new Address());
        when(shipments.findById(2L)).thenReturn(Optional.of(shipment));
        return shipment;
    }

    @Test
    void assignedCourierCanReadDetailsAndHistory() {
        User courier = viewer(Role.COURIER, true);
        assignedShipment(courier);
        when(parcels.findAllByShipmentId(2L)).thenReturn(List.of());
        when(events.findAllByShipmentIdOrderByOccurredAtAscIdAsc(2L)).thenReturn(List.of());
        assertEquals(2L, shipmentService.getShipmentById(2L, "viewer@example.test").id());
        assertTrue(shipmentService.getTrackingEvents(2L, "viewer@example.test").isEmpty());
    }

    @Test
    void anotherCourierCannotReadDetailsOrHistory() {
        viewer(Role.COURIER, true);
        User assigned = new User(); assigned.setId(8L); assignedShipment(assigned);
        assertThrows(AccessDeniedException.class, () -> shipmentService.getShipmentById(2L, "viewer@example.test"));
        assertThrows(AccessDeniedException.class, () -> shipmentService.getTrackingEvents(2L, "viewer@example.test"));
        verifyNoInteractions(parcels, events);
    }

    @Test
    void inactiveAssignedCourierCannotReadDetails() {
        User courier = viewer(Role.COURIER, false); assignedShipment(courier);
        assertThrows(AccessDeniedException.class, () -> shipmentService.getShipmentById(2L, "viewer@example.test"));
        verifyNoInteractions(parcels, events);
    }
}
