package hu.kolozsvaripeter.parceltracker.controller;

import hu.kolozsvaripeter.parceltracker.exception.*;
import jakarta.servlet.Filter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig({MyShipmentsControllerTest.TestConfig.class, ApiErrorControllerTest.Config.class})
@WebAppConfiguration
class ApiErrorControllerTest {
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter filter;
    MockMvc mvc;
    @BeforeEach void setup() { mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build(); }

    @ParameterizedTest
    @CsvSource({"user,404,USER_NOT_FOUND", "shipment,404,SHIPMENT_NOT_FOUND", "state,409,SHIPMENT_STATE_CONFLICT",
        "access,409,USER_ACCESS_CONFLICT", "email,409,EMAIL_ALREADY_EXISTS", "database,409,DATA_CONFLICT",
        "argument,400,INVALID_REQUEST", "auth,401,UNAUTHORIZED", "forbidden,403,FORBIDDEN", "unexpected,500,INTERNAL_ERROR"})
    void controllerFailuresUseSafeContract(String kind, int expected, String code) throws Exception {
        mvc.perform(get("/api/error-test/"+kind).session(session()))
            .andExpect(status().is(expected)).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.errorCode").value(code)).andExpect(jsonPath("$.message").isNotEmpty())
            .andExpect(jsonPath("$.fieldErrors").isArray()).andExpect(jsonPath("$.details").doesNotExist())
            .andExpect(content().string(not(containsString("PRIVATE_DATABASE_PASSWORD"))));
    }
    @Test void security401UsesSameContract() throws Exception {
        mvc.perform(get("/api/shipments/my")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").isNotEmpty()).andExpect(jsonPath("$.fieldErrors").isEmpty());
    }
    @Test void security403UsesSameContract() throws Exception {
        mvc.perform(get("/api/admin/users").session(session())).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN")).andExpect(jsonPath("$.fieldErrors").isEmpty());
    }
    @Test void csrf403UsesSameContract() throws Exception {
        mvc.perform(post("/api/error-test").session(session()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
            .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }
    @Test void validationHasFieldPathButNeverRejectedValue() throws Exception {
        mvc.perform(csrfPost("{\"name\":\"\"}")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
            .andExpect(jsonPath("$.fieldErrors[0].message").isNotEmpty())
            .andExpect(jsonPath("$.fieldErrors[0].rejectedValue").doesNotExist());
    }
    @Test void malformedJsonIsBadRequest() throws Exception {
        mvc.perform(csrfPost("{\"name\":PRIVATE_DATABASE_PASSWORD")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
            .andExpect(content().string(not(containsString("PRIVATE_DATABASE_PASSWORD"))));
    }
    @Test void unsupportedMethodKeepsAllowHeader() throws Exception {
        mvc.perform(put("/api/error-test/test").session(csrfSession).header(csrfHeader,csrfToken))
            .andExpect(status().isMethodNotAllowed()).andExpect(header().string("Allow",containsString("GET")))
            .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }
    MockHttpSession csrfSession;
    String csrfHeader, csrfToken;
    @BeforeEach void csrf() {
        csrfSession=session(); var request=new MockHttpServletRequest(); request.setSession(csrfSession);
        var repo=new HttpSessionCsrfTokenRepository();var token=repo.generateToken(request);
        repo.saveToken(token,request,new MockHttpServletResponse());
        new XorCsrfTokenRequestAttributeHandler().handle(request,new MockHttpServletResponse(),()->token);
        var masked=(CsrfToken)request.getAttribute(CsrfToken.class.getName());
        csrfHeader=masked.getHeaderName();csrfToken=masked.getToken();
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder csrfPost(String body) {
        return post("/api/error-test").session(csrfSession).header(csrfHeader,csrfToken).contentType(MediaType.APPLICATION_JSON).content(body);
    }
    private MockHttpSession session() {
        var auth=new UsernamePasswordAuthenticationToken("user@test.hu",null,List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        var ctx=SecurityContextHolder.createEmptyContext();ctx.setAuthentication(auth);
        var session=new MockHttpSession();session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,ctx);return session;
    }
    @Configuration static class Config { @Bean Probe probe() {return new Probe();} }
    @RestController static class Probe {
        record Command(@NotBlank String name) {}
        @PostMapping("/api/error-test") void validate(@Valid @RequestBody Command command) {}
        @GetMapping("/api/error-test/{kind}") void fail(@PathVariable String kind) {
            String secret="PRIVATE_DATABASE_PASSWORD";
            throw switch(kind) {
                case "user" -> new UserNotFoundException(secret);
                case "shipment" -> new ShipmentNotFoundException(secret);
                case "state" -> new ShipmentStateConflictException(secret);
                case "access" -> new UserAccessConflictException(secret);
                case "email" -> new DuplicateEmailException();
                case "database" -> new DataIntegrityViolationException(secret);
                case "argument" -> new IllegalArgumentException(secret);
                case "auth" -> new BadCredentialsException(secret);
                case "forbidden" -> new AccessDeniedException(secret);
                default -> new IllegalStateException(secret);
            };
        }
    }
}
