package hu.kolozsvaripeter.parceltracker.dto.outgoing;

public record AddressDetailsDto(
        String country,
        String postalCode,
        String city,
        String street,
        String houseNumber,
        String door
) {
}