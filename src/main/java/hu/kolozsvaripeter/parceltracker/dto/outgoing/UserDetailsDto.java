package hu.kolozsvaripeter.parceltracker.dto.outgoing;

import hu.kolozsvaripeter.parceltracker.enums.Role;

public record UserDetailsDto(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        Role role,
        boolean active,
        AddressDetailsDto address
) {

    public UserDetailsDto(
            Long id,
            String fullName,
            String email,
            String phoneNumber,
            Role role,
            boolean active
    ) {
        this(id, fullName, email, phoneNumber, role, active, null);
    }
}
