package hu.kolozsvaripeter.parceltracker.controller;

import hu.kolozsvaripeter.parceltracker.dto.incoming.UpdateShipmentStatusDto;
import hu.kolozsvaripeter.parceltracker.exception.GlobalExceptionHandler;
import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import hu.kolozsvaripeter.parceltracker.exception.ShipmentNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import hu.kolozsvaripeter.parceltracker.exception.ShipmentStateConflictException;
import hu.kolozsvaripeter.parceltracker.service.ShipmentService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig({MyShipmentsControllerTest.TestConfig.class, UpdateShipmentStatusControllerTest.AdviceConfig.class})
@WebAppConfiguration
class UpdateShipmentStatusControllerTest {
    @Autowired WebApplicationContext context;
    @Autowired ShipmentService service;
    @Autowired @Qualifier("springSecurityFilterChain") Filter securityFilterChain;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(service);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilterChain).build();
    }

    @Test
    void courierWithCsrfCanRequestStatusChange() throws Exception {
        mvc.perform(request("COURIER", "{\"status\":\"PICKED_UP\"}"))
                .andExpect(status().isNoContent());
        verify(service).updateShipmentStatus(2L,
                new UpdateShipmentStatusDto(ShipmentStatus.PICKED_UP, null), "viewer@example.test");
    }

    @Test
    void adminWithCsrfCanRequestCancellation() throws Exception {
        mvc.perform(request("ADMIN", "{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isNoContent());
        verify(service).updateShipmentStatus(2L,
                new UpdateShipmentStatusDto(ShipmentStatus.CANCELLED, null), "viewer@example.test");
    }

    @Test
    void customerIsDeniedBeforeServiceCall() throws Exception {
        mvc.perform(request("CUSTOMER", "{\"status\":\"PICKED_UP\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void anonymousWithValidCsrfReceives401() throws Exception {
        mvc.perform(request(null, "{\"status\":\"PICKED_UP\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void missingCsrfReceives403() throws Exception {
        mvc.perform(patch("/api/shipments/2/status").session(session("COURIER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PICKED_UP\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":null}", "{\"status\":\"UNKNOWN\"}", "{"})
    void invalidRequestReceives400(String body) throws Exception {
        mvc.perform(request("COURIER", body)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void descriptionLongerThan500CharactersReceives400() throws Exception {
        String body = "{\"status\":\"PICKED_UP\",\"description\":\"" + "x".repeat(501) + "\"}";
        mvc.perform(request("COURIER", body)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void forbiddenStatusTransitionReceives409() throws Exception {
        doThrow(new ShipmentStateConflictException("Invalid status transition"))
                .when(service).updateShipmentStatus(2L,
                        new UpdateShipmentStatusDto(ShipmentStatus.DELIVERED, null), "viewer@example.test");
        mvc.perform(request("COURIER", "{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void unassignedCourierReceives403FromService() throws Exception {
        doThrow(new AccessDeniedException("Access denied"))
                .when(service).updateShipmentStatus(2L,
                        new UpdateShipmentStatusDto(ShipmentStatus.PICKED_UP, null), "viewer@example.test");
        mvc.perform(request("COURIER", "{\"status\":\"PICKED_UP\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingShipmentReceives404() throws Exception {
        doThrow(new ShipmentNotFoundException("Shipment not found"))
                .when(service).updateShipmentStatus(2L,
                        new UpdateShipmentStatusDto(ShipmentStatus.PICKED_UP, null), "viewer@example.test");
        mvc.perform(request("COURIER", "{\"status\":\"PICKED_UP\"}"))
                .andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder request(String role, String body) {
        var session = session(role);
        var request = new MockHttpServletRequest();
        request.setSession(session);
        var repository = new HttpSessionCsrfTokenRepository();
        var token = repository.generateToken(request);
        repository.saveToken(token, request, new MockHttpServletResponse());
        new XorCsrfTokenRequestAttributeHandler().handle(request, new MockHttpServletResponse(), () -> token);
        var maskedToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return patch("/api/shipments/2/status").session(session)
                .header(maskedToken.getHeaderName(), maskedToken.getToken())
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpSession session(String role) {
        var session = new MockHttpSession();
        if (role != null) {
            var authentication = new UsernamePasswordAuthenticationToken("viewer@example.test", null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            var securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        }
        return session;
    }

    @Configuration
    static class AdviceConfig {
        @Bean GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler(new StaticMessageSource());
        }
    }
}
