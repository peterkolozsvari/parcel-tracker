package hu.kolozsvaripeter.parceltracker.dto.outgoing;

import java.math.BigDecimal;

public record ParcelDetailsDto(
        Long id,
        BigDecimal weight,
        String description,
        boolean fragile
) {
}