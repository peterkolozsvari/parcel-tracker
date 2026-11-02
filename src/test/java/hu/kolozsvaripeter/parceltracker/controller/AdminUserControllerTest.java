package hu.kolozsvaripeter.parceltracker.controller;
import hu.kolozsvaripeter.parceltracker.service.AdminUserService;
import hu.kolozsvaripeter.parceltracker.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import jakarta.servlet.Filter;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig({MyShipmentsControllerTest.TestConfig.class, AdminUserControllerTest.Config.class})
@WebAppConfiguration
class AdminUserControllerTest {
    @Autowired WebApplicationContext context;
    @Autowired AdminUserService service;
    @Autowired @Qualifier("springSecurityFilterChain") Filter filter;
    MockMvc mvc;
    @BeforeEach void setup() {
        reset(service);
        mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build();
    }
    @Test void anonymousListIsUnauthorized() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }
    @Test void customerAndCourierCannotListUsers() throws Exception {
        for (String role : List.of("CUSTOMER","COURIER"))
            mvc.perform(get("/api/admin/users").session(session(role))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void adminCanListUsers() throws Exception {
        when(service.getUsers(any(),any(),any(),eq("actor@example.test")))
            .thenReturn(org.springframework.data.domain.Page.empty(org.springframework.data.domain.PageRequest.of(0,10)));
        mvc.perform(get("/api/admin/users").session(session("ADMIN"))).andExpect(status().isOk());
    }
    @Test void invalidFilterIsBadRequest() throws Exception {
        mvc.perform(get("/api/admin/users").param("role", "ROOT").session(session("ADMIN")))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void accessMutationRequiresCsrf() throws Exception {
        mvc.perform(patch("/api/admin/users/2/access").session(session("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"COURIER\",\"active\":true}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void validMutationPassesAuthenticatedIdentity() throws Exception {
        mvc.perform(mutation(HttpMethod.PATCH,"/api/admin/users/2/access","ADMIN","{\"role\":\"COURIER\",\"active\":true}"))
            .andExpect(status().isOk());
        verify(service).updateAccess(eq(2L),any(),eq("actor@example.test"));
    }
    @Test void missingFieldsAndInvalidRoleAreRejected() throws Exception {
        for (String body : List.of("{}", "{\"role\":\"ROOT\",\"active\":true}"))
            mvc.perform(mutation(HttpMethod.PATCH,"/api/admin/users/2/access","ADMIN",body)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void customerCannotChangeRoleEvenWithCsrf() throws Exception {
        mvc.perform(mutation(HttpMethod.PATCH,"/api/admin/users/2/access","CUSTOMER","{\"role\":\"ADMIN\",\"active\":true}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(service);
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


    @Configuration static class Config {
        @Bean AdminUserService adminService() { return mock(AdminUserService.class); }
        @Bean AdminUserController controller(AdminUserService service) { return new AdminUserController(service); }
        @Bean hu.kolozsvaripeter.parceltracker.exception.AdminUserExceptionHandler adminHandler() {
            return new hu.kolozsvaripeter.parceltracker.exception.AdminUserExceptionHandler();
        }
        @Bean GlobalExceptionHandler handler() { return new GlobalExceptionHandler(new StaticMessageSource()); }
    }
}
