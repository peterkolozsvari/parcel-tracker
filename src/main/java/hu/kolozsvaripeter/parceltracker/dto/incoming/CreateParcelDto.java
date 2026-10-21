package hu.kolozsvaripeter.parceltracker.dto.incoming;


import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateParcelDto(

        @NotNull(message = "Weight is required")
        @DecimalMin(
                value = "0",
                inclusive = false,
                message = "Weight must be greater than zero"
        )
        @Digits(
                integer = 8,
                fraction = 2,
                message = "Enter the parcel weight in kilograms with up to two decimal places."
        )
        BigDecimal weight,

        @Size(
                max = 500,
                message = "Description must not exceed 500 characters"
        )
        String description,

        boolean fragile
) {
}
