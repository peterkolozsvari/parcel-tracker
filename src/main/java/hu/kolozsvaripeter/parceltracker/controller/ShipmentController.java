package hu.kolozsvaripeter.parceltracker.controller;

import hu.kolozsvaripeter.parceltracker.dto.incoming.CreateShipmentDto;
import hu.kolozsvaripeter.parceltracker.dto.incoming.AssignCourierDto;
import hu.kolozsvaripeter.parceltracker.dto.incoming.UpdateShipmentStatusDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.CreateShipmentResponseDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.ShipmentDetailsDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.ShipmentListItemDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.TrackingEventDto;
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

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private static final Logger log = LoggerFactory.getLogger(ShipmentController.class);

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<CreateShipmentResponseDto> createShipment(@RequestBody @Valid CreateShipmentDto command,
                                                                   Principal principal) {
        log.info("create Shipment: {}", command);
        CreateShipmentResponseDto response =
                shipmentService.createShipment(command, principal.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/courier")
    public ResponseEntity<Void> assignCourier(@PathVariable Long id,
                                             @RequestBody @Valid AssignCourierDto command,
                                             Principal principal) {
        shipmentService.assignCourier(id, command, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateShipmentStatus(@PathVariable Long id,
                                                    @RequestBody @Valid UpdateShipmentStatusDto command,
                                                    Principal principal) {
        shipmentService.updateShipmentStatus(id, command, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentDetailsDto> getShipmentById(@PathVariable Long id, Principal principal) {
        log.info("Shipment is requested: {}", id);
        ShipmentDetailsDto shipment = shipmentService.getShipmentById(id, principal.getName());
        return new ResponseEntity<>(shipment, HttpStatus.OK);
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ShipmentDetailsDto> getShipmentByTrackingNumber(@PathVariable String trackingNumber, Principal principal) {
        log.info("Shipment is requested: {}", trackingNumber);
        ShipmentDetailsDto shipment = shipmentService.getShipmentByTrackingNumber(trackingNumber, principal.getName());
        return new ResponseEntity<>(shipment, HttpStatus.OK);
    }

    @GetMapping("/{id}/tracking-events")
    public List<TrackingEventDto> getTrackingEvents(@PathVariable Long id, Principal principal) {
        return shipmentService.getTrackingEvents(id, principal.getName());
    }

    @GetMapping("/assigned")
    public Page<ShipmentListItemDto> getAssignedShipments(
            Principal principal,
            @RequestParam(required = false) ShipmentStatus status,
            @PageableDefault(size = 10, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return shipmentService.getAssignedShipments(principal.getName(), status, pageable);
    }

    @GetMapping("/my")
    public Page<ShipmentListItemDto> getMyShipments(
            Principal principal,
            @RequestParam(required = false) ShipmentStatus status,
            @PageableDefault(
                    size = 10,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return shipmentService.getMyShipments(principal.getName(), status, pageable);
    }

    @GetMapping
    public Page<ShipmentListItemDto> getShipments(
            Principal principal,
            @RequestParam(required = false) ShipmentStatus status,
            @RequestParam(required = false) Long senderId,
            @PageableDefault(
                    size = 10,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return shipmentService.getShipments(status, senderId, pageable, principal.getName());
    }
}
