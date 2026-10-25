package hu.kolozsvaripeter.parceltracker.controller;

import hu.kolozsvaripeter.parceltracker.dto.incoming.CreateShipmentDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.CreateShipmentResponseDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.ShipmentDetailsDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.ShipmentListItemDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.UserDetailsDto;
import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import hu.kolozsvaripeter.parceltracker.service.ShipmentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private static final Logger log = LoggerFactory.getLogger(ShipmentController.class);

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<CreateShipmentResponseDto> createShipment(@RequestBody @Valid CreateShipmentDto command) {
        log.info("create Shipment: {}", command);
        CreateShipmentResponseDto response =
                shipmentService.createShipment(command);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentDetailsDto> getShipmentById(@PathVariable Long id) {
        log.info("Shipment is requested: {}", id);
        ShipmentDetailsDto shipment = shipmentService.getShipmentById(id);
        return new ResponseEntity<>(shipment, HttpStatus.OK);
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ShipmentDetailsDto> getShipmentByTrackingNumber(@PathVariable String trackingNumber) {
        log.info("Shipment is requested: {}", trackingNumber);
        ShipmentDetailsDto shipment = shipmentService.getShipmentByTrackingNumber(trackingNumber);
        return new ResponseEntity<>(shipment, HttpStatus.OK);
    }

    @GetMapping
    public Page<ShipmentListItemDto> getShipments(
            @RequestParam(required = false) ShipmentStatus status,
            @RequestParam(required = false) Long senderId,
            @PageableDefault(
                    size = 10,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return shipmentService.getShipments(status, senderId, pageable);
    }
}