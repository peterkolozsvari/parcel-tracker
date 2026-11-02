package hu.kolozsvaripeter.parceltracker.controller;

import hu.kolozsvaripeter.parceltracker.config.SecurityConfig;
import hu.kolozsvaripeter.parceltracker.enums.ShipmentStatus;
import hu.kolozsvaripeter.parceltracker.service.ShipmentService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(MyShipmentsControllerTest.TestConfig.class)
@WebAppConfiguration
class MyShipmentsControllerTest {
    @Autowired WebApplicationContext context;
    @Autowired ShipmentService shipmentService;
    @Autowired @Qualifier("springSecurityFilterChain") Filter securityFilterChain;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(shipmentService);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilterChain).build();
    }

    @Test
    void anonymousRequestReturns401() throws Exception {
        mvc.perform(get("/api/shipments/my")).andExpect(status().isUnauthorized());
        verifyNoInteractions(shipmentService);
    }

    @Test
    void usesSessionEmailAndIgnoresClientSuppliedSenderId() throws Exception {
        when(shipmentService.getMyShipments(eq("user@example.test"), isNull(), any()))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));

        mvc.perform(get("/api/shipments/my").session(session("CUSTOMER"))
                        .param("senderId", "999"))
                .andExpect(status().isOk());

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(shipmentService).getMyShipments(eq("user@example.test"), isNull(), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(10, pageable.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "createdAt", "id"), pageable.getValue().getSort());
    }

    @Test
    void acceptsStatusFilterAndPageParameters() throws Exception {
        when(shipmentService.getMyShipments(eq("user@example.test"), eq(ShipmentStatus.REGISTERED), any()))
                .thenReturn(Page.empty(PageRequest.of(1, 5)));

        mvc.perform(get("/api/shipments/my").session(session("CUSTOMER"))
                        .param("status", "REGISTERED").param("page", "1").param("size", "5"))
                .andExpect(status().isOk());

        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(shipmentService).getMyShipments(eq("user@example.test"), eq(ShipmentStatus.REGISTERED), pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
    }

    @Test
    void customerCannotBypassOwnListUsingGlobalList() throws Exception {
        mvc.perform(get("/api/shipments").session(session("CUSTOMER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(shipmentService);
    }

    @Test
    void adminCanStillUseGlobalList() throws Exception {
        when(shipmentService.getShipments(isNull(), isNull(), any(), eq("user@example.test")))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));
        mvc.perform(get("/api/shipments").session(session("ADMIN")))
                .andExpect(status().isOk());
        verify(shipmentService).getShipments(isNull(), isNull(), any(), eq("user@example.test"));
    }

    private MockHttpSession session(String role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "user@example.test", null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfig implements WebMvcConfigurer {
        @Bean ShipmentService shipmentService() { return mock(ShipmentService.class); }
        @Bean ShipmentController shipmentController(ShipmentService service) { return new ShipmentController(service); }
        @Bean UserDetailsService userDetailsService() {
            return username -> User.withUsername(username).password("unused").roles("CUSTOMER").build();
        }
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new PageableHandlerMethodArgumentResolver());
        }
    }
}
