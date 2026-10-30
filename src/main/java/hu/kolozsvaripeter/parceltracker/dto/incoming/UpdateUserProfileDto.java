package hu.kolozsvaripeter.parceltracker.dto.incoming;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileDto(
        @Pattern(
                regexp = "(?s).*\\S.*",
                message = "Full name must not be blank"
        )
        @Size(
                max = 100,
                message = "Full name must not exceed 100 characters"
        )
        String fullName,

        @Size(
                max = 30,
                message = "Phone number must not exceed 30 characters"
        )
        String phoneNumber,

        @Valid
        CreateAddressDto address
) {
}