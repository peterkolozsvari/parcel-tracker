package hu.kolozsvaripeter.parceltracker.dto.incoming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAddressDto (

        @NotBlank(message = "Country is required")
        @Size(
                max = 100,
                message = "Country name must not exceed 100 characters"
        )
        String country,


        @NotBlank(message = "Postal code is required")
        @Size(
                max = 20,
                message = "Postal code must not exceed 20 characters"
        )
        String postalCode,


        @NotBlank(message = "City name is required")
        @Size(
                max = 100,
                message = "City name must not exceed 100 characters"
        )
        String city,


        @NotBlank(message = "Street name is required")
        @Size(
                max = 150,
                message = "Street name must not exceed 150 characters"
        )
        String street,


        @NotBlank(message = "House number is required")
        @Size(
                max = 20,
                message = "House number must not exceed 20 characters"
        )
        String houseNumber,


        @Size(
                max = 50,
                message = "Door number must not exceed 50 characters"
        )
        String door) {
}
