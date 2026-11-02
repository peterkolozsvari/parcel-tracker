package hu.kolozsvaripeter.parceltracker.dto.incoming;

import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateShipmentStatusDto(
        @NotNull ShipmentStatus status,
        @Size(max = 500) String description
) {
}
