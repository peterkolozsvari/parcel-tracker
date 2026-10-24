package hu.kolozsvaripeter.parceltracker.controller;

import hu.kolozsvaripeter.parceltracker.dto.incoming.CreateShipmentDto;
import hu.kolozsvaripeter.parceltracker.dto.outgoing.CreateShipmentResponseDto;
import hu.kolozsvaripeter.parceltracker.service.ShipmentService;
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

}
