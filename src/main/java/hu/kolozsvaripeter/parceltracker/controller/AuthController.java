package hu.kolozsvaripeter.parceltracker.controller;

import hu.kolozsvaripeter.parceltracker.dto.incoming.LoginDto;
import hu.kolozsvaripeter.parceltracker.service.AuthService;
import hu.kolozsvaripeter.parceltracker.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }


    @PostMapping
    public ResponseEntity<Void> loginUser(@RequestBody @Valid LoginDto command) {
        log.info("User logged in: {}", command.email());

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
