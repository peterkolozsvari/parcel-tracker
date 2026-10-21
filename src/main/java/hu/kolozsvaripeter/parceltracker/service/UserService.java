package hu.kolozsvaripeter.parceltracker.service;

import hu.kolozsvaripeter.parceltracker.domain.User;
import hu.kolozsvaripeter.parceltracker.dto.incoming.CreateUserCommandDto;
import hu.kolozsvaripeter.parceltracker.dto.incoming.UpdateUserProfileDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.UserDetailsDto;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import hu.kolozsvaripeter.parceltracker.exception.UserNotFoundException;
import hu.kolozsvaripeter.parceltracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public void createUser(CreateUserCommandDto command) {
        if (userRepository.existsByEmailIgnoreCase(command.email())) {
            throw new IllegalArgumentException(
                    "A user with this email already exists");
        }

        String passwordHash = passwordEncoder.encode(command.password());

        User user = new User(
                command.email(),
                passwordHash,
                command.fullName(),
                command.phoneNumber(),
                Role.CUSTOMER
        );

        userRepository.save(user);
    }

    public UserDetailsDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        return new UserDetailsDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isActive()
        );
    }

    public void updateUser(Long id, UpdateUserProfileDto command) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        if (command.fullName() != null) {
            user.setFullName(command.fullName());
        }
        if (command.phoneNumber() != null) {
            user.setPhoneNumber(command.phoneNumber());
        }

        userRepository.save(user);
    }
}
