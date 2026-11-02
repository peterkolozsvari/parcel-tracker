package hu.kolozsvaripeter.parceltracker.controller;

import hu.kolozsvaripeter.parceltracker.exception.GlobalExceptionHandler;
import hu.kolozsvaripeter.parceltracker.exception.ShipmentNotFoundException;
import hu.kolozsvaripeter.parceltracker.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShipmentDetailsControllerTest {
    private ShipmentService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ShipmentService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ShipmentController(service))
                .setControllerAdvice(new GlobalExceptionHandler(new StaticMessageSource()))
                .build();
    }

    @Test
    void usesPrincipalInsteadOfClientSuppliedEmail() throws Exception {
        mvc.perform(get("/api/shipments/2").principal(() -> "owner@example.test")
                        .param("email", "someone-else@example.test"))
                .andExpect(status().isOk());
        verify(service).getShipmentById(2L, "owner@example.test");
    }

    @Test
    void deniedAccessReturns403InsteadOfGeneric500() throws Exception {
        when(service.getShipmentById(2L, "other@example.test"))
                .thenThrow(new AccessDeniedException("Access denied"));
        mvc.perform(get("/api/shipments/2").principal(() -> "other@example.test"))
                .andExpect(status().isForbidden());
    }

    @Test
    void trackingLookupAlsoUsesPrincipalAndEnforcesAccess() throws Exception {
        when(service.getShipmentByTrackingNumber("TEST234567", "other@example.test"))
                .thenThrow(new AccessDeniedException("Access denied"));
        mvc.perform(get("/api/shipments/tracking/TEST234567").principal(() -> "other@example.test"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingShipmentReturns404() throws Exception {
        when(service.getShipmentById(999L, "owner@example.test"))
                .thenThrow(new ShipmentNotFoundException("Shipment not found"));
        mvc.perform(get("/api/shipments/999").principal(() -> "owner@example.test"))
                .andExpect(status().isNotFound());
    }

    @Test
    void trackingEventsUsePrincipalAndIgnoreClientEmail() throws Exception {
        mvc.perform(get("/api/shipments/2/tracking-events").principal(() -> "owner@example.test")
                        .param("email", "someone-else@example.test"))
                .andExpect(status().isOk());
        verify(service).getTrackingEvents(2L, "owner@example.test");
    }

    @Test
    void trackingEventsDenyAnotherUsersAccess() throws Exception {
        when(service.getTrackingEvents(2L, "other@example.test"))
                .thenThrow(new AccessDeniedException("Access denied"));
        mvc.perform(get("/api/shipments/2/tracking-events").principal(() -> "other@example.test"))
                .andExpect(status().isForbidden());
    }

    @Test
    void trackingEventsForMissingShipmentReturn404() throws Exception {
        when(service.getTrackingEvents(999L, "owner@example.test"))
                .thenThrow(new ShipmentNotFoundException("Shipment not found"));
        mvc.perform(get("/api/shipments/999/tracking-events").principal(() -> "owner@example.test"))
                .andExpect(status().isNotFound());
    }
}
