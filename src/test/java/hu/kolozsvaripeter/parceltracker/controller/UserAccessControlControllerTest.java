package hu.kolozsvaripeter.parceltracker.controller;

import hu.kolozsvaripeter.parceltracker.domain.User;
import hu.kolozsvaripeter.parceltracker.dto.incoming.CreateShipmentDto;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import hu.kolozsvaripeter.parceltracker.exception.GlobalExceptionHandler;
import hu.kolozsvaripeter.parceltracker.repository.AddressRepository;
import hu.kolozsvaripeter.parceltracker.repository.UserRepository;
import hu.kolozsvaripeter.parceltracker.service.*;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig({MyShipmentsControllerTest.TestConfig.class, UserAccessControlControllerTest.ProfileConfig.class})
@WebAppConfiguration
class UserAccessControlControllerTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired AddressRepository addresses;
    @Autowired ShipmentService shipments;
    @Autowired @Qualifier("springSecurityFilterChain") Filter securityFilterChain;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(users, addresses, shipments);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilterChain).build();
    }

    private User actor(Role role, boolean active) {
        User user = new User(); user.setId(1L); user.setRole(role); user.setActive(active);
        user.setFullName("Actor"); user.setEmail("actor@example.test");
        when(users.findByEmailIgnoreCase("actor@example.test")).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void ownProfileCanBeRead() throws Exception {
        actor(Role.CUSTOMER, true);
        mvc.perform(get("/api/users/1").session(session("CUSTOMER"))).andExpect(status().isOk());
    }

    @Test
    void anotherProfileCannotBeReadEvenWithForgedEmailParameter() throws Exception {
        actor(Role.CUSTOMER, true);
        mvc.perform(get("/api/users/2").session(session("CUSTOMER")).param("email", "other@example.test"))
                .andExpect(status().isForbidden());
        verify(users, never()).findById(any());
    }

    @Test
    void anotherProfileCannotBeModifiedEvenWithValidCsrf() throws Exception {
        actor(Role.CUSTOMER, true);
        mvc.perform(mutation(HttpMethod.PATCH, "/api/users/2", "CUSTOMER", "{\"fullName\":\"Tampered\"}"))
                .andExpect(status().isForbidden());
        verify(users, never()).save(any());
        verifyNoInteractions(addresses);
    }

    @Test
    void ownProfileCanBeModified() throws Exception {
        User actor = actor(Role.CUSTOMER, true);
        mvc.perform(mutation(HttpMethod.PATCH, "/api/users/1", "CUSTOMER", "{\"fullName\":\"Updated\"}"))
                .andExpect(status().isOk());
        assertEquals("Updated", actor.getFullName());
        assertEquals(Role.CUSTOMER, actor.getRole());
        verify(users).save(actor);
    }

    @Test
    void adminCanReadAnotherProfile() throws Exception {
        actor(Role.ADMIN, true);
        User target = new User(); target.setId(2L); target.setRole(Role.CUSTOMER);
        when(users.findById(2L)).thenReturn(Optional.of(target));
        mvc.perform(get("/api/users/2").session(session("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void inactiveExistingSessionCannotReadProfile() throws Exception {
        actor(Role.CUSTOMER, false);
        mvc.perform(get("/api/users/1").session(session("CUSTOMER"))).andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotReadProfile() throws Exception {
        mvc.perform(get("/api/users/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(users);
    }

    @Test
    void profileMutationWithoutCsrfIsDenied() throws Exception {
        mvc.perform(patch("/api/users/1").session(session("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Updated\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(users);
    }

    @Test
    void creationUsesSessionPrincipalAndPropagatesAccessDenied() throws Exception {
        when(shipments.createShipment(any(CreateShipmentDto.class), eq("actor@example.test")))
                .thenThrow(new AccessDeniedException("Forged sender"));
        String body = """
                {"senderId":2,"receiverName":"Teszt Címzett",
                 "pickupAddress":{"country":"HU","postalCode":"1211","city":"Budapest","street":"Teszt utca","houseNumber":"1"},
                 "deliveryAddress":{"country":"HU","postalCode":"1211","city":"Budapest","street":"Teszt utca","houseNumber":"2"},
                 "parcels":[{"weight":1.5,"fragile":false}]}
                """;
        mvc.perform(mutation(HttpMethod.POST, "/api/shipments", "CUSTOMER", body)).andExpect(status().isForbidden());
        verify(shipments).createShipment(any(CreateShipmentDto.class), eq("actor@example.test"));
    }

    @Test
    void inactiveAccountCannotRestoreSession() throws Exception {
        actor(Role.CUSTOMER, false);
        mvc.perform(get("/api/auth/me").session(session("CUSTOMER"))).andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder mutation(HttpMethod method, String path, String role, String body) {
        var session = session(role);
        var request = new MockHttpServletRequest(); request.setSession(session);
        var repository = new HttpSessionCsrfTokenRepository();
        var token = repository.generateToken(request);
        repository.saveToken(token, request, new MockHttpServletResponse());
        new XorCsrfTokenRequestAttributeHandler().handle(request, new MockHttpServletResponse(), () -> token);
        var masked = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return request(method, path).session(session).header(masked.getHeaderName(), masked.getToken())
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpSession session(String role) {
        var authentication = new UsernamePasswordAuthenticationToken("actor@example.test", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        var securityContext = SecurityContextHolder.createEmptyContext(); securityContext.setAuthentication(authentication);
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        return session;
    }

    @Configuration
    static class ProfileConfig {
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean AddressRepository addresses() { return mock(AddressRepository.class); }
        @Bean UserService userService(UserRepository users, AddressRepository addresses) {
            return new UserService(users, mock(PasswordEncoder.class), addresses);
        }
        @Bean UserController userController(UserService service) { return new UserController(service); }
        @Bean AuthService authService(AuthenticationManager manager, UserRepository users) { return new AuthService(manager, users); }
        @Bean AuthController authController(AuthService service) { return new AuthController(service); }
        @Bean GlobalExceptionHandler exceptionHandler() { return new GlobalExceptionHandler(new StaticMessageSource()); }
    }
}
