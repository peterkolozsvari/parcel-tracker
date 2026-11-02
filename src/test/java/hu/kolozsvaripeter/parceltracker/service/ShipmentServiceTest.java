package hu.kolozsvaripeter.parceltracker.service;

import hu.kolozsvaripeter.parceltracker.domain.Address;
import hu.kolozsvaripeter.parceltracker.domain.Parcel;
import hu.kolozsvaripeter.parceltracker.domain.Shipment;
import hu.kolozsvaripeter.parceltracker.domain.User;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.AddressDetailsDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.ParcelDetailsDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.ShipmentDetailsDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.UserDetailsDto;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import hu.kolozsvaripeter.parceltracker.exception.ShipmentNotFoundException;
import hu.kolozsvaripeter.parceltracker.repository.AddressRepository;
import hu.kolozsvaripeter.parceltracker.repository.ParcelRepository;
import hu.kolozsvaripeter.parceltracker.repository.ShipmentRepository;
import hu.kolozsvaripeter.parceltracker.repository.TrackingEventRepository;
import hu.kolozsvaripeter.parceltracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ParcelRepository parcelRepository;

    @Mock
    private TrackingEventRepository trackingEventRepository;

    @InjectMocks
    private ShipmentService shipmentService;

    private Shipment shipment;
    private Parcel parcel;
    private User courier;

    @BeforeEach
    void setUp() {
        User sender = new User();
        sender.setId(5L);
        sender.setFullName("Kovács Anna");
        sender.setEmail("anna@example.test");
        sender.setPhoneNumber(null);
        sender.setRole(Role.CUSTOMER);
        sender.setActive(true);

        courier = new User();
        courier.setId(7L);
        courier.setFullName("Szabó Tamás");
        courier.setEmail("tamas@example.test");
        courier.setPhoneNumber(null);
        courier.setRole(Role.COURIER);
        courier.setActive(true);

        Address pickupAddress = new Address();
        pickupAddress.setCountry("Magyarország");
        pickupAddress.setPostalCode("1211");
        pickupAddress.setCity("Budapest");
        pickupAddress.setStreet("Fő utca");
        pickupAddress.setHouseNumber("12");
        pickupAddress.setDoor("2. emelet 3.");

        Address deliveryAddress = new Address();
        deliveryAddress.setCountry("Magyarország");
        deliveryAddress.setPostalCode("6720");
        deliveryAddress.setCity("Szeged");
        deliveryAddress.setStreet("Kossuth utca");
        deliveryAddress.setHouseNumber("8");
        deliveryAddress.setDoor(null);

        shipment = new Shipment();
        shipment.setId(2L);
        shipment.setTrackingNumber("TEST234567");
        shipment.setSender(sender);
        shipment.setCourier(courier);
        shipment.setReceiverName("Tóth Katalin");
        shipment.setPickupAddress(pickupAddress);
        shipment.setDeliveryAddress(deliveryAddress);
        shipment.setStatus(ShipmentStatus.COURIER_ASSIGNED);
        shipment.setCreatedAt(
                OffsetDateTime.parse("2026-10-25T10:00:00Z")
        );
        shipment.setUpdatedAt(
                OffsetDateTime.parse("2026-10-25T11:00:00Z")
        );

        parcel = new Parcel();
        parcel.setId(10L);
        parcel.setShipment(shipment);
        parcel.setWeight(new BigDecimal("2.50"));
        parcel.setDescription("Bögre");
        parcel.setFragile(true);
    }

    @Test
    void getShipmentByIdReturnsShipmentDetails() {
        when(userRepository.findByEmailIgnoreCase("anna@example.test"))
                .thenReturn(Optional.of(shipment.getSender()));
        // Arrange
        when(shipmentRepository.findById(2L))
                .thenReturn(Optional.of(shipment));

        when(parcelRepository.findAllByShipmentId(2L))
                .thenReturn(List.of(parcel));

        // Act
        ShipmentDetailsDto result = shipmentService.getShipmentById(2L, "anna@example.test");

        // Assert
        assertEquals(expectedDetails(), result);
    }

    @Test
    void getShipmentByTrackingNumberReturnsShipmentDetails() {
        when(userRepository.findByEmailIgnoreCase("anna@example.test"))
                .thenReturn(Optional.of(shipment.getSender()));
        // Arrange
        when(shipmentRepository.findByTrackingNumber("TEST234567"))
                .thenReturn(Optional.of(shipment));

        when(parcelRepository.findAllByShipmentId(2L))
                .thenReturn(List.of(parcel));

        // Act
        ShipmentDetailsDto result =
                shipmentService.getShipmentByTrackingNumber("TEST234567", "anna@example.test");

        // Assert
        assertEquals(expectedDetails(), result);
    }

    @Test
    void getShipmentByIdThrowsWhenShipmentDoesNotExist() {
        // Arrange
        when(shipmentRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act and assert
        ShipmentNotFoundException exception = assertThrows(
                ShipmentNotFoundException.class,
                () -> shipmentService.getShipmentById(999L, "anna@example.test")
        );

        assertEquals(
                "Shipment not found with id: 999",
                exception.getMessage()
        );

        verifyNoInteractions(parcelRepository);
    }

    @Test
    void getShipmentByTrackingNumberThrowsWhenShipmentDoesNotExist() {
        // Arrange
        when(shipmentRepository.findByTrackingNumber("MISSING234"))
                .thenReturn(Optional.empty());

        // Act and assert
        assertThrows(
                ShipmentNotFoundException.class,
                () -> shipmentService.getShipmentByTrackingNumber("MISSING234", "anna@example.test")
        );

        verifyNoInteractions(parcelRepository);
    }

    @Test
    void getShipmentByIdReturnsNullCourierWhenNotAssigned() {
        when(userRepository.findByEmailIgnoreCase("anna@example.test"))
                .thenReturn(Optional.of(shipment.getSender()));
        // Arrange
        shipment.setCourier(null);
        shipment.setStatus(ShipmentStatus.REGISTERED);

        when(shipmentRepository.findById(2L))
                .thenReturn(Optional.of(shipment));

        when(parcelRepository.findAllByShipmentId(2L))
                .thenReturn(List.of(parcel));

        // Act
        ShipmentDetailsDto result = shipmentService.getShipmentById(2L, "anna@example.test");

        // Assert
        assertNull(result.courier());
        assertEquals(ShipmentStatus.REGISTERED, result.status());
        assertEquals(2L, result.id());
        assertEquals(1, result.parcels().size());
    }

    @Test
    void anotherCustomerCannotReadShipmentById() {
        User other = new User();
        other.setId(99L);
        other.setRole(Role.CUSTOMER);
        when(userRepository.findByEmailIgnoreCase("other@example.test")).thenReturn(Optional.of(other));
        when(shipmentRepository.findById(2L)).thenReturn(Optional.of(shipment));

        assertThrows(AccessDeniedException.class,
                () -> shipmentService.getShipmentById(2L, "other@example.test"));
        verifyNoInteractions(parcelRepository);
    }

    @Test
    void anotherCustomerCannotReadShipmentByTrackingNumber() {
        User other = new User();
        other.setId(99L);
        other.setRole(Role.CUSTOMER);
        when(userRepository.findByEmailIgnoreCase("other@example.test")).thenReturn(Optional.of(other));
        when(shipmentRepository.findByTrackingNumber("TEST234567")).thenReturn(Optional.of(shipment));

        assertThrows(AccessDeniedException.class,
                () -> shipmentService.getShipmentByTrackingNumber("TEST234567", "other@example.test"));
        verifyNoInteractions(parcelRepository);
    }

    @Test
    void adminCanReadAnotherUsersShipment() {
        User admin = new User();
        admin.setId(99L);
        admin.setRole(Role.ADMIN);
        when(userRepository.findByEmailIgnoreCase("admin@example.test")).thenReturn(Optional.of(admin));
        when(shipmentRepository.findById(2L)).thenReturn(Optional.of(shipment));
        when(parcelRepository.findAllByShipmentId(2L)).thenReturn(List.of(parcel));

        assertEquals(expectedDetails(), shipmentService.getShipmentById(2L, "admin@example.test"));
    }

    private ShipmentDetailsDto expectedDetails() {
        return new ShipmentDetailsDto(
                2L,
                "TEST234567",
                ShipmentStatus.COURIER_ASSIGNED,
                new UserDetailsDto(
                        5L,
                        "Kovács Anna",
                        "anna@example.test",
                        null,
                        Role.CUSTOMER,
                        true
                ),
                new UserDetailsDto(
                        7L,
                        "Szabó Tamás",
                        "tamas@example.test",
                        null,
                        Role.COURIER,
                        true
                ),
                "Tóth Katalin",
                new AddressDetailsDto(
                        "Magyarország",
                        "1211",
                        "Budapest",
                        "Fő utca",
                        "12",
                        "2. emelet 3."
                ),
                new AddressDetailsDto(
                        "Magyarország",
                        "6720",
                        "Szeged",
                        "Kossuth utca",
                        "8",
                        null
                ),
                List.of(
                        new ParcelDetailsDto(
                                10L,
                                new BigDecimal("2.50"),
                                "Bögre",
                                true
                        )
                ),
                OffsetDateTime.parse("2026-10-25T10:00:00Z"),
                OffsetDateTime.parse("2026-10-25T11:00:00Z")
        );
    }
}
