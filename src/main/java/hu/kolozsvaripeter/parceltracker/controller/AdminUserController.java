package hu.kolozsvaripeter.parceltracker.controller;

import hu.kolozsvaripeter.parceltracker.dto.incoming.UpdateUserAccessDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.AdminUserDto;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import hu.kolozsvaripeter.parceltracker.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService service;
    public AdminUserController(AdminUserService service) { this.service = service; }

    @GetMapping
    public Page<AdminUserDto> getUsers(@RequestParam(required = false) Role role,
                                      @RequestParam(required = false) Boolean active,
                                      Pageable pageable, Principal principal) {
        return service.getUsers(role, active, pageable, principal.getName());
    }

    @PatchMapping("/{id}/access")
    public AdminUserDto updateAccess(@PathVariable Long id, @RequestBody @Valid UpdateUserAccessDto command,
                                     Principal principal) {
        return service.updateAccess(id, command, principal.getName());
    }
}
