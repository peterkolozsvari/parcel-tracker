package hu.kolozsvaripeter.parceltracker.dto.outgoing;

import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import java.time.OffsetDateTime;

public record TrackingEventDto(
        Long id,
        ShipmentStatus status,
        String description,
        OffsetDateTime occurredAt
) {
}
