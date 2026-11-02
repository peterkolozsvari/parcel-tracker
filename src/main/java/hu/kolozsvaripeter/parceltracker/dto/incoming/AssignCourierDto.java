package hu.kolozsvaripeter.parceltracker.dto.incoming;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignCourierDto(
        @NotNull @Positive Long courierId
) {
}
