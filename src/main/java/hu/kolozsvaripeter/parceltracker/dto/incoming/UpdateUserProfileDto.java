package hu.kolozsvaripeter.parceltracker.dto.incoming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileDto (
        @NotBlank(message = "Full name is required")
        @Size(
                max = 100,
                message = "Full name must not exceed 100 characters"
        )
        String fullName,
        @Size(
                max = 30,
                message = "Phone number must not exceed 30 characters"
        )
        String phoneNumber
) {
}
