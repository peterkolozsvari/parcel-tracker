package hu.kolozsvaripeter.parceltracker.dto.outgoing;

import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record ShipmentDetailsDto(
        Long id,
        String trackingNumber,
        ShipmentStatus status,
        UserDetailsDto sender,
        UserDetailsDto courier,
        String receiverName,
        AddressDetailsDto pickupAddress,
        AddressDetailsDto deliveryAddress,
        List<ParcelDetailsDto> parcels,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}