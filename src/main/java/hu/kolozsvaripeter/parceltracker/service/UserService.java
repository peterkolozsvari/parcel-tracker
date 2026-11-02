package hu.kolozsvaripeter.parceltracker.service;

import hu.kolozsvaripeter.parceltracker.domain.Address;
import hu.kolozsvaripeter.parceltracker.domain.User;
import hu.kolozsvaripeter.parceltracker.dto.incoming.CreateUserCommandDto;
import hu.kolozsvaripeter.parceltracker.dto.incoming.UpdateUserProfileDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.UserDetailsDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.CourierListItemDto;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import hu.kolozsvaripeter.parceltracker.exception.UserNotFoundException;
import hu.kolozsvaripeter.parceltracker.repository.AddressRepository;
import hu.kolozsvaripeter.parceltracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;


@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AddressRepository addressRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AddressRepository addressRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.addressRepository = addressRepository;
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

    @Transactional(readOnly = true)
    public List<CourierListItemDto> getActiveCouriers(String email) {
        User admin = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        if (!admin.isActive() || admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied");
        }
        return userRepository.findAllByRoleAndActiveTrueOrderByFullNameAscIdAsc(Role.COURIER)
                .stream().map(user -> new CourierListItemDto(user.getId(), user.getFullName())).toList();
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

        Address address = user.getAddress();

        if (address == null) {
            address = new Address();
        }

        if (command.fullName() != null) {
            user.setFullName(command.fullName());
        }
        if (command.phoneNumber() != null) {
            user.setPhoneNumber(command.phoneNumber());
        }

        if (command.address() != null) {
            address.setCountry(command.address().country());
            address.setPostalCode(command.address().postalCode());
            address.setCity(command.address().city());
            address.setStreet(command.address().street());
            address.setHouseNumber(command.address().houseNumber());
            address.setDoor(command.address().door());
            user.setAddress(addressRepository.save(address));
        }

        userRepository.save(user);
    }
}

