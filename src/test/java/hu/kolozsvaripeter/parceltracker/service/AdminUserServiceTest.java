package hu.kolozsvaripeter.parceltracker.service;

import hu.kolozsvaripeter.parceltracker.domain.User;
import hu.kolozsvaripeter.parceltracker.dto.incoming.UpdateUserAccessDto;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import hu.kolozsvaripeter.parceltracker.exception.*;
import hu.kolozsvaripeter.parceltracker.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {
    @Mock UserRepository users;
    @InjectMocks AdminUserService service;
    User actor(Role role, boolean active) {
        User user = new User("admin@test.hu", "secret", "Admin", null, role);
        user.setId(1L); user.setActive(active);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        return user;
    }
    @Test void listIsBoundedFilteredAndContainsNoPassword() {
        actor(Role.ADMIN, true);
        User target = new User("target@test.hu", "secret", "Target", null, Role.COURIER);
        when(users.findByAccessFilters(eq(Role.COURIER), eq(true), any())).thenReturn(new PageImpl<>(List.of(target)));
        var result = service.getUsers(Role.COURIER, true, PageRequest.of(2, 500), "admin@test.hu");
        assertEquals("target@test.hu", result.getContent().getFirst().email());
        verify(users).findByAccessFilters(Role.COURIER, true, PageRequest.of(2, 100, Sort.by("fullName", "id")));
    }
    @ParameterizedTest @EnumSource(value=Role.class, names="ADMIN", mode=EnumSource.Mode.EXCLUDE)
    void nonAdminCannotManageUsers(Role role) {
        actor(role, true);
        assertThrows(AccessDeniedException.class, () -> service.getUsers(null, null, PageRequest.of(0,10), "admin@test.hu"));
        assertThrows(AccessDeniedException.class, () -> service.updateAccess(2L, new UpdateUserAccessDto(Role.ADMIN,true), "admin@test.hu"));
        verify(users, never()).save(any());
    }
    @Test void disabledAdminCannotManageUsers() {
        actor(Role.ADMIN, false);
        assertThrows(AccessDeniedException.class, () -> service.updateAccess(2L,new UpdateUserAccessDto(Role.ADMIN,true),"admin@test.hu"));
        verify(users, never()).findByIdForUpdate(any());
    }
    @ParameterizedTest @EnumSource(Role.class)
    void adminCanAssignAnyRoleToAnotherUser(Role role) {
        actor(Role.ADMIN, true);
        User target = new User(); target.setId(2L);
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(target));
        when(users.save(target)).thenReturn(target);
        var result = service.updateAccess(2L,new UpdateUserAccessDto(role,false),"admin@test.hu");
        assertEquals(role,result.role()); assertFalse(result.active());
        var order = inOrder(users);
        order.verify(users).lockUsersByRole(Role.ADMIN);
        order.verify(users).findByEmailIgnoreCase("admin@test.hu");
        order.verify(users).findByIdForUpdate(2L);
    }
    @Test void cannotDisableOrDemoteSelf() {
        User admin = actor(Role.ADMIN,true);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        assertThrows(UserAccessConflictException.class, () -> service.updateAccess(1L,new UpdateUserAccessDto(Role.ADMIN,false),"admin@test.hu"));
        assertThrows(UserAccessConflictException.class, () -> service.updateAccess(1L,new UpdateUserAccessDto(Role.CUSTOMER,true),"admin@test.hu"));
        assertTrue(admin.isActive()); assertEquals(Role.ADMIN,admin.getRole());
        verify(users,never()).save(any());
    }
    @Test void missingTargetReturnsNotFound() {
        actor(Role.ADMIN,true);
        assertThrows(UserNotFoundException.class, () -> service.updateAccess(99L,new UpdateUserAccessDto(Role.CUSTOMER,true),"admin@test.hu"));
    }
}
