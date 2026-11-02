package hu.kolozsvaripeter.parceltracker.dto.outgoing;

import hu.kolozsvaripeter.parceltracker.enums.Role;

public record AdminUserDto(Long id, String fullName, String email, String phoneNumber,
                           Role role, boolean active) {}
