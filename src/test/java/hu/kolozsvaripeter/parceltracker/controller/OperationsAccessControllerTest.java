package hu.kolozsvaripeter.parceltracker.controller;

import hu.kolozsvaripeter.parceltracker.dto.outgoing.CourierListItemDto;
import hu.kolozsvaripeter.parceltracker.service.ShipmentService;
import hu.kolozsvaripeter.parceltracker.service.UserService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig({MyShipmentsControllerTest.TestConfig.class, OperationsAccessControllerTest.DirectoryConfig.class})
@WebAppConfiguration
class OperationsAccessControllerTest {
    @Autowired WebApplicationContext context;
    @Autowired ShipmentService shipments;
    @Autowired UserService users;
    @Autowired @Qualifier("springSecurityFilterChain") Filter securityFilterChain;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(shipments, users);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilterChain).build();
    }

    @Test
    void courierListUsesSessionIdentityAndIgnoresCourierIdParameter() throws Exception {
        when(shipments.getAssignedShipments(eq("viewer@example.test"), isNull(), any()))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));
        mvc.perform(get("/api/shipments/assigned").session(session("COURIER")).param("courierId", "999"))
                .andExpect(status().isOk());
        verify(shipments).getAssignedShipments(eq("viewer@example.test"), isNull(), any());
    }

    @Test
    void customerCannotReadCourierList() throws Exception {
        mvc.perform(get("/api/shipments/assigned").session(session("CUSTOMER"))).andExpect(status().isForbidden());
        verifyNoInteractions(shipments);
    }

    @Test
    void anonymousCannotReadCourierList() throws Exception {
        mvc.perform(get("/api/shipments/assigned")).andExpect(status().isUnauthorized());
        verifyNoInteractions(shipments);
    }

    @Test
    void adminCanReadActiveCourierDirectory() throws Exception {
        when(users.getActiveCouriers("viewer@example.test")).thenReturn(List.of(new CourierListItemDto(7L, "Teszt Futár")));
        mvc.perform(get("/api/users/couriers").session(session("ADMIN"))).andExpect(status().isOk());
        verify(users).getActiveCouriers("viewer@example.test");
    }

    @Test
    void courierCannotReadAdminDirectory() throws Exception {
        mvc.perform(get("/api/users/couriers").session(session("COURIER"))).andExpect(status().isForbidden());
        verifyNoInteractions(users);
    }

    @Test
    void customerCannotReadAdminDirectory() throws Exception {
        mvc.perform(get("/api/users/couriers").session(session("CUSTOMER"))).andExpect(status().isForbidden());
        verifyNoInteractions(users);
    }

    private MockHttpSession session(String role) {
        var authentication = new UsernamePasswordAuthenticationToken("viewer@example.test", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        var securityContext = SecurityContextHolder.createEmptyContext(); securityContext.setAuthentication(authentication);
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }

    @Configuration
    static class DirectoryConfig {
        @Bean UserService userService() { return mock(UserService.class); }
        @Bean UserController userController(UserService service) { return new UserController(service); }
    }
}
