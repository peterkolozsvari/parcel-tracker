package hu.kolozsvaripeter.parceltracker.service;

import hu.kolozsvaripeter.parceltracker.domain.*;
import hu.kolozsvaripeter.parceltracker.dto.incoming.CreateAddressDto;
import hu.kolozsvaripeter.parceltracker.dto.incoming.CreateParcelDto;
import hu.kolozsvaripeter.parceltracker.dto.incoming.CreateShipmentDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.*;
import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import hu.kolozsvaripeter.parceltracker.exception.ShipmentNotFoundException;
import hu.kolozsvaripeter.parceltracker.exception.UserNotFoundException;
import hu.kolozsvaripeter.parceltracker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

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
        String characters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
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

    public ShipmentDetailsDto getShipmentById(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment not found with id: " + id
                ));

        List<ParcelDetailsDto> parcels = parcelRepository
                .findAllByShipmentId(id)
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
