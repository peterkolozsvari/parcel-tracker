package hu.kolozsvaripeter.parceltracker.controller;

import hu.kolozsvaripeter.parceltracker.dto.incoming.AssignCourierDto;
import hu.kolozsvaripeter.parceltracker.exception.GlobalExceptionHandler;
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

@SpringJUnitConfig({MyShipmentsControllerTest.TestConfig.class, AssignCourierControllerTest.AdviceConfig.class})
@WebAppConfiguration
class AssignCourierControllerTest {
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
    void adminWithCsrfCanAssignCourier() throws Exception {
        mvc.perform(request("ADMIN", "{\"courierId\":7}"))
                .andExpect(status().isNoContent());
        verify(service).assignCourier(2L, new AssignCourierDto(7L), "viewer@example.test");
    }

    @Test
    void customerWithValidCsrfStillReceives403() throws Exception {
        mvc.perform(request("CUSTOMER", "{\"courierId\":7}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void anonymousWithValidCsrfReceives401() throws Exception {
        mvc.perform(request(null, "{\"courierId\":7}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void missingCsrfReceives403() throws Exception {
        mvc.perform(patch("/api/shipments/2/courier").session(session("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"courierId\":7}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void missingCourierIdReceives400() throws Exception {
        mvc.perform(request("ADMIN", "{}" )).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void nonPositiveCourierIdReceives400() throws Exception {
        mvc.perform(request("ADMIN", "{\"courierId\":0}" )).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void invalidShipmentStateReceives409() throws Exception {
        doThrow(new ShipmentStateConflictException("Already assigned"))
                .when(service).assignCourier(2L, new AssignCourierDto(7L), "viewer@example.test");
        mvc.perform(request("ADMIN", "{\"courierId\":7}" )).andExpect(status().isConflict());
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
        return patch("/api/shipments/2/courier").session(session)
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
