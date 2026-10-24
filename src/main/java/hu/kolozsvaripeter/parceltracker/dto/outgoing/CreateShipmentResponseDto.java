package hu.kolozsvaripeter.parceltracker.dto.outgoing;

public record CreateShipmentResponseDto(
        Long id,
        String trackingNumber
) {
}
