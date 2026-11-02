package hu.kolozsvaripeter.parceltracker.service;

import hu.kolozsvaripeter.parceltracker.domain.User;
import hu.kolozsvaripeter.parceltracker.dto.incoming.UpdateUserAccessDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.AdminUserDto;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import hu.kolozsvaripeter.parceltracker.exception.UserAccessConflictException;
import hu.kolozsvaripeter.parceltracker.exception.UserNotFoundException;
import hu.kolozsvaripeter.parceltracker.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminUserService {
    private final UserRepository users;

    public AdminUserService(UserRepository users) { this.users = users; }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> getUsers(Role role, Boolean active, Pageable pageable, String email) {
        requireAdmin(email);
        // Fixed, deterministic order and bounded page size for the management list.
        Pageable bounded = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100),
                Sort.by("fullName", "id"));
        return users.findByAccessFilters(role, active, bounded).map(this::toDto);
    }

    public AdminUserDto updateAccess(Long id, UpdateUserAccessDto command, String email) {
        // Serialize administrator changes so two admins cannot concurrently disable each other.
        users.lockUsersByRole(Role.ADMIN);
        User actor = requireAdmin(email);
        User target = users.findByIdForUpdate(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (actor.getId().equals(id) && (!command.active() || command.role() != Role.ADMIN)) {
            throw new UserAccessConflictException("You cannot remove your own administrator access");
        }
        target.setRole(command.role());
        target.setActive(command.active());
        return toDto(users.save(target));
    }

    private User requireAdmin(String email) {
        User actor = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
        if (!actor.isActive() || actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied");
        }
        return actor;
    }

    private AdminUserDto toDto(User user) {
        return new AdminUserDto(user.getId(), user.getFullName(), user.getEmail(),
                user.getPhoneNumber(), user.getRole(), user.isActive());
    }
}
