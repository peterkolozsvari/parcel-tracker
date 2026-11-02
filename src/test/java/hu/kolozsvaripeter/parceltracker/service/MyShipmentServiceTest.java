package hu.kolozsvaripeter.parceltracker.service;

import hu.kolozsvaripeter.parceltracker.domain.Shipment;
import hu.kolozsvaripeter.parceltracker.domain.User;
import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import hu.kolozsvaripeter.parceltracker.exception.UserNotFoundException;
import hu.kolozsvaripeter.parceltracker.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyShipmentServiceTest {
    @Mock UserRepository userRepository;
    @Mock AddressRepository addressRepository;
    @Mock ShipmentRepository shipmentRepository;
    @Mock ParcelRepository parcelRepository;
    @Mock TrackingEventRepository trackingEventRepository;
    @InjectMocks ShipmentService shipmentService;

    @Test
    void usesAuthenticatedUsersIdAndPreservesFilterPagingAndMapping() {
        User user = new User();
        user.setId(11L);
        when(userRepository.findByEmailIgnoreCase("user@example.test"))
                .thenReturn(Optional.of(user));
        var pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Shipment shipment = new Shipment();
        shipment.setId(25L);
        shipment.setTrackingNumber("TEST123456");
        shipment.setReceiverName("Teszt Címzett");
        shipment.setStatus(ShipmentStatus.REGISTERED);
        shipment.setCreatedAt(OffsetDateTime.parse("2026-11-02T10:00:00Z"));
        when(shipmentRepository.findAllByFilters(ShipmentStatus.REGISTERED, 11L, pageable))
                .thenReturn(new PageImpl<>(List.of(shipment), pageable, 11));

        var result = shipmentService.getMyShipments("user@example.test", ShipmentStatus.REGISTERED, pageable);

        assertEquals(1, result.getNumber());
        assertEquals(2, result.getTotalPages());
        assertEquals(11, result.getTotalElements());
        var item = result.getContent().getFirst();
        assertEquals(shipment.getId(), item.id());
        assertEquals(shipment.getTrackingNumber(), item.trackingNumber());
        assertEquals(shipment.getReceiverName(), item.receiverName());
        assertEquals(shipment.getStatus(), item.status());
        assertEquals(shipment.getCreatedAt(), item.createdAt());
        verify(shipmentRepository).findAllByFilters(ShipmentStatus.REGISTERED, 11L, pageable);
    }

    @Test
    void returnsEmptyPageForUserWithoutShipments() {
        User user = new User();
        user.setId(12L);
        var pageable = PageRequest.of(0, 10);
        when(userRepository.findByEmailIgnoreCase("other@example.test"))
                .thenReturn(Optional.of(user));
        when(shipmentRepository.findAllByFilters(null, 12L, pageable))
                .thenReturn(Page.empty(pageable));

        var result = shipmentService.getMyShipments("other@example.test", null, pageable);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(shipmentRepository).findAllByFilters(null, 12L, pageable);
    }

    @Test
    void doesNotQueryShipmentsWhenAuthenticatedUserNoLongerExists() {
        when(userRepository.findByEmailIgnoreCase("missing@example.test"))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> shipmentService.getMyShipments(
                "missing@example.test", null, PageRequest.of(0, 10)));
        verifyNoInteractions(shipmentRepository);
    }
}
