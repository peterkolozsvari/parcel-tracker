package hu.kolozsvaripeter.parceltracker.dto.incoming;

import hu.kolozsvaripeter.parceltracker.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserAccessDto(@NotNull Role role, @NotNull Boolean active) {}
