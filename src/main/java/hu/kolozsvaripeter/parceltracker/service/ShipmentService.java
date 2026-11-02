package hu.kolozsvaripeter.parceltracker.service;

import hu.kolozsvaripeter.parceltracker.domain.*;
import hu.kolozsvaripeter.parceltracker.dto.incoming.CreateAddressDto;
import hu.kolozsvaripeter.parceltracker.dto.incoming.CreateParcelDto;
import hu.kolozsvaripeter.parceltracker.dto.incoming.CreateShipmentDto;
import hu.kolozsvaripeter.parceltracker.dto.incoming.AssignCourierDto;
import hu.kolozsvaripeter.parceltracker.dto.incoming.UpdateShipmentStatusDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.*;
import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import hu.kolozsvaripeter.parceltracker.exception.ShipmentNotFoundException;
import hu.kolozsvaripeter.parceltracker.exception.ShipmentStateConflictException;
import hu.kolozsvaripeter.parceltracker.exception.UserNotFoundException;
import hu.kolozsvaripeter.parceltracker.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
@Transactional
public class ShipmentService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ShipmentRepository shipmentRepository;
    private final ParcelRepository parcelRepository;
    private final TrackingEventRepository trackingEventRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    public ShipmentService(UserRepository userRepository, AddressRepository addressRepository, ShipmentRepository shipmentRepository, ParcelRepository parcelRepository, TrackingEventRepository trackingEventRepository) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.shipmentRepository = shipmentRepository;
        this.parcelRepository = parcelRepository;
        this.trackingEventRepository = trackingEventRepository;
    }


    public CreateShipmentResponseDto createShipment(CreateShipmentDto command) {
        User sender = findSender(command.senderId());

        Address pickupAddress = saveAddress(command.pickupAddress());
        Address deliveryAddress = saveAddress(command.deliveryAddress());

        Shipment shipment = saveShipment(command, sender, pickupAddress, deliveryAddress);

        saveParcels(command.parcels(), shipment);
        saveInitialTrackingEvent(shipment, sender);

        return new CreateShipmentResponseDto(shipment.getId(), shipment.getTrackingNumber());
    }

    public void assignCourier(Long shipmentId, AssignCourierDto command, String email) {
        User admin = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        if (!admin.isActive() || admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied");
        }

        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found with id: " + shipmentId));
        if (shipment.getStatus() != ShipmentStatus.REGISTERED || shipment.getCourier() != null) {
            throw new ShipmentStateConflictException("Only an unassigned registered shipment can be assigned to a courier");
        }

        User courier = userRepository.findById(command.courierId())
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + command.courierId()));
        if (!courier.isActive() || courier.getRole() != Role.COURIER) {
            throw new IllegalArgumentException("The selected user must be an active courier");
        }

        shipment.setCourier(courier);
        shipment.setStatus(ShipmentStatus.COURIER_ASSIGNED);
        shipmentRepository.save(shipment);

        TrackingEvent event = new TrackingEvent();
        event.setShipment(shipment);
        event.setChangedByUser(admin);
        event.setStatus(ShipmentStatus.COURIER_ASSIGNED);
        event.setDescription("Futár hozzárendelve a küldeményhez.");
        trackingEventRepository.save(event);
    }

    public void updateShipmentStatus(Long shipmentId, UpdateShipmentStatusDto command, String email) {
        User actor = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        if (!actor.isActive() || (actor.getRole() != Role.ADMIN && actor.getRole() != Role.COURIER)) {
            throw new AccessDeniedException("Access denied");
        }

        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found with id: " + shipmentId));
        if (actor.getRole() == Role.COURIER && (shipment.getCourier() == null
                || !actor.getId().equals(shipment.getCourier().getId())
                || command.status() == ShipmentStatus.CANCELLED)) {
            throw new AccessDeniedException("Access denied");
        }

        if (!isAllowedStatusTransition(shipment.getStatus(), command.status())
                || (command.status() != ShipmentStatus.CANCELLED && shipment.getCourier() == null)) {
            throw new ShipmentStateConflictException(
                    "Shipment status cannot change from " + shipment.getStatus() + " to " + command.status());
        }

        shipment.setStatus(command.status());
        shipmentRepository.save(shipment);

        TrackingEvent event = new TrackingEvent();
        event.setShipment(shipment);
        event.setChangedByUser(actor);
        event.setStatus(command.status());
        event.setDescription(command.description() == null || command.description().isBlank()
                ? defaultStatusDescription(command.status()) : command.description().strip());
        trackingEventRepository.save(event);
    }

    private boolean isAllowedStatusTransition(ShipmentStatus current, ShipmentStatus next) {
        return switch (current) {
            case REGISTERED -> next == ShipmentStatus.CANCELLED;
            case COURIER_ASSIGNED -> next == ShipmentStatus.PICKED_UP || next == ShipmentStatus.CANCELLED;
            case PICKED_UP -> next == ShipmentStatus.IN_TRANSIT;
            case IN_TRANSIT -> next == ShipmentStatus.DELIVERED || next == ShipmentStatus.DELIVERY_FAILED;
            case DELIVERY_FAILED -> next == ShipmentStatus.IN_TRANSIT;
            case DELIVERED, CANCELLED -> false;
        };
    }

    private String defaultStatusDescription(ShipmentStatus status) {
        return switch (status) {
            case PICKED_UP -> "A futár felvette a küldeményt.";
            case IN_TRANSIT -> "A küldemény szállítás alatt van.";
            case DELIVERED -> "A küldeményt kézbesítettük.";
            case DELIVERY_FAILED -> "A kézbesítés sikertelen volt.";
            case CANCELLED -> "A küldeményt töröltük.";
            default -> throw new IllegalArgumentException("Unsupported target status: " + status);
        };
    }

    @Transactional(readOnly = true)
    public ShipmentDetailsDto getShipmentById(Long id, String email) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment not found with id: " + id
                ));

        checkShipmentAccess(shipment, email);
        return toShipmentDetailsDto(shipment);
    }

    @Transactional(readOnly = true)
    public ShipmentDetailsDto getShipmentByTrackingNumber(String trackingNumber, String email) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment not found with tracking number: " + trackingNumber
                ));

        checkShipmentAccess(shipment, email);
        return toShipmentDetailsDto(shipment);
    }

    @Transactional(readOnly = true)
    public List<TrackingEventDto> getTrackingEvents(Long shipmentId, String email) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found with id: " + shipmentId));
        checkShipmentAccess(shipment, email);

        return trackingEventRepository.findAllByShipmentIdOrderByOccurredAtAscIdAsc(shipmentId)
                .stream()
                .map(event -> new TrackingEventDto(
                        event.getId(), event.getStatus(), event.getDescription(), event.getOccurredAt()))
                .toList();
    }

    private void checkShipmentAccess(Shipment shipment, String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        boolean assignedCourier = user.getRole() == Role.COURIER && shipment.getCourier() != null
                && user.getId().equals(shipment.getCourier().getId());
        if (!user.isActive() || (user.getRole() != Role.ADMIN
                && !user.getId().equals(shipment.getSender().getId()) && !assignedCourier)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    @Transactional(readOnly = true)
    public Page<ShipmentListItemDto> getMyShipments(String email, ShipmentStatus status, Pageable pageable) {
        User sender = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        return getShipments(status, sender.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<ShipmentListItemDto> getAssignedShipments(String email, ShipmentStatus status, Pageable pageable) {
        User courier = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        if (!courier.isActive() || courier.getRole() != Role.COURIER) {
            throw new AccessDeniedException("Access denied");
        }
        return shipmentRepository.findAssignedToCourier(courier.getId(), status, pageable)
                .map(shipment -> new ShipmentListItemDto(shipment.getId(), shipment.getTrackingNumber(),
                        shipment.getReceiverName(), shipment.getStatus(), shipment.getCreatedAt()));
    }

    public Page<ShipmentListItemDto> getShipments(ShipmentStatus status, Long senderId, Pageable pageable) {
        return shipmentRepository.findAllByFilters(status, senderId, pageable)
                .map(shipment -> new ShipmentListItemDto(
                        shipment.getId(),
                        shipment.getTrackingNumber(),
                        shipment.getReceiverName(),
                        shipment.getStatus(),
                        shipment.getCreatedAt()
                ));
    }

    private ShipmentDetailsDto toShipmentDetailsDto(Shipment shipment) {
        List<ParcelDetailsDto> parcels = parcelRepository
                .findAllByShipmentId(shipment.getId())
                .stream()
                .map(this::toParcelDetailsDto)
                .toList();

        return new ShipmentDetailsDto(
                shipment.getId(),
                shipment.getTrackingNumber(),
                shipment.getStatus(),
                toUserDetailsDto(shipment.getSender()),
                shipment.getCourier() == null
                        ? null
                        : toUserDetailsDto(shipment.getCourier()),
                shipment.getReceiverName(),
                toAddressDetailsDto(shipment.getPickupAddress()),
                toAddressDetailsDto(shipment.getDeliveryAddress()),
                parcels,
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }

    private User findSender(Long senderId) {
        return userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found with id: " + senderId
                ));
    }

    private Address saveAddress(CreateAddressDto dto) {
        Address address = new Address();
        address.setCountry(dto.country());
        address.setPostalCode(dto.postalCode());
        address.setCity(dto.city());
        address.setStreet(dto.street());
        address.setHouseNumber(dto.houseNumber());
        address.setDoor(dto.door());

        return addressRepository.save(address);
    }

    private Shipment saveShipment(
            CreateShipmentDto command,
            User sender,
            Address pickupAddress,
            Address deliveryAddress) {

        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(generateTrackingNumber());
        shipment.setSender(sender);
        shipment.setPickupAddress(pickupAddress);
        shipment.setDeliveryAddress(deliveryAddress);
        shipment.setReceiverName(command.receiverName());
        shipment.setStatus(ShipmentStatus.REGISTERED);

        return shipmentRepository.save(shipment);
    }

    private void saveParcels(
            List<CreateParcelDto> parcelDtos,
            Shipment shipment) {

        List<Parcel> parcels = parcelDtos.stream()
                .map(dto -> {
                    Parcel parcel = new Parcel();
                    parcel.setShipment(shipment);
                    parcel.setWeight(dto.weight());
                    parcel.setDescription(dto.description());
                    parcel.setFragile(dto.fragile());
                    return parcel;
                })
                .toList();

        parcelRepository.saveAll(parcels);
    }

    private void saveInitialTrackingEvent(Shipment shipment, User sender) {
        TrackingEvent event = new TrackingEvent();
        event.setShipment(shipment);
        event.setChangedByUser(sender);
        event.setStatus(ShipmentStatus.REGISTERED);
        event.setDescription("Shipment registered");

        trackingEventRepository.save(event);
    }

    private String generateTrackingNumber(){
        String characters = "ABCDEFGHJKLMNPQRSTUVWXYZ123456789";
        StringBuilder trackingNumber = new StringBuilder();

        for (int i = 0; i < 10; i++) {

            int index = RANDOM.nextInt(characters.length());
            char character = characters.charAt(index);
            trackingNumber.append(character);
        }
        if (shipmentRepository.existsByTrackingNumber(trackingNumber.toString())) {
            return generateTrackingNumber();
        }
        return trackingNumber.toString();
    }


    private UserDetailsDto toUserDetailsDto(User user) {
        return new UserDetailsDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isActive()
        );
    }

    private AddressDetailsDto toAddressDetailsDto(Address address) {
        return new AddressDetailsDto(
                address.getCountry(),
                address.getPostalCode(),
                address.getCity(),
                address.getStreet(),
                address.getHouseNumber(),
                address.getDoor()
        );
    }

    private ParcelDetailsDto toParcelDetailsDto(Parcel parcel) {
        return new ParcelDetailsDto(
                parcel.getId(),
                parcel.getWeight(),
                parcel.getDescription(),
                parcel.isFragile()
        );
    }
}

