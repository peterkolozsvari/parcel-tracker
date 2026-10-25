package hu.kolozsvaripeter.parceltracker.dto.outgoing;

import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import java.time.OffsetDateTime;

public record ShipmentListItemDto (
        Long id,
        String trackingNumber,
        String receiverName,
        ShipmentStatus status,
        OffsetDateTime createdAt
) {
}
