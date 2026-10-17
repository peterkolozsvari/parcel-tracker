package hu.kolozsvaripeter.parceltracker.controller;

import hu.kolozsvaripeter.parceltracker.dto.incoming.CreateUserCommandDto;
import hu.kolozsvaripeter.parceltracker.dto.incoming.UpdateUserProfileDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.UserDetailsDto;
import hu.kolozsvaripeter.parceltracker.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Void> createUser(@RequestBody @Valid CreateUserCommandDto command) {
        log.info("create User: {}", command.fullName());
        userService.createUser(command);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDetailsDto> getUserById(@PathVariable Long id) {
        log.info("User is requested: {}", id);
        UserDetailsDto user = userService.getUserById(id);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateUser(@PathVariable Long id, @RequestBody @Valid  UpdateUserProfileDto command) {
        log.info("update User: {}", id);
        userService.updateUser(id, command);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
