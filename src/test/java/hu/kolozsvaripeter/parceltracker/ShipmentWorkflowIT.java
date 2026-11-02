package hu.kolozsvaripeter.parceltracker;

import hu.kolozsvaripeter.parceltracker.domain.User;
import hu.kolozsvaripeter.parceltracker.enums.Role;
import hu.kolozsvaripeter.parceltracker.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.*;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP, cookies, Spring Security, JPA and Flyway against an isolated PostgreSQL schema. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShipmentWorkflowIT {
    static final String URL = System.getenv().getOrDefault("POSTGRES_TEST_URL", "jdbc:postgresql://localhost:55432/parcel_tracker_test");
    static final String DB_USER = System.getenv().getOrDefault("POSTGRES_TEST_USER", "postgres");
    static final String DB_PASSWORD = System.getenv().getOrDefault("POSTGRES_TEST_PASSWORD", "test-only-password");
    static final String SCHEMA = "it_" + UUID.randomUUID().toString().replace("-", "");
    static final String PASSWORD = "Test-only-42!";
    static final JsonMapper JSON = JsonMapper.builder().build();
    @LocalServerPort int port;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired hu.kolozsvaripeter.parceltracker.service.ShipmentService shipments;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        String database = URI.create(URL.substring("jdbc:".length())).getPath();
        if (database == null || !database.endsWith("_test")) {
            throw new IllegalArgumentException("Integration tests require a dedicated database whose name ends in _test");
        }
        properties.add("spring.datasource.url", () -> URL + (URL.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        properties.add("spring.datasource.username", () -> DB_USER);
        properties.add("spring.datasource.password", () -> DB_PASSWORD);
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
    }

    @BeforeEach void resetData() {
        jdbc.execute("TRUNCATE TABLE tracking_events, parcels, shipments, users, addresses RESTART IDENTITY CASCADE");
    }

    @AfterAll static void removeTestSchema() throws Exception {
        try (var connection = DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
             var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        }
    }

    @Test void migrationsAndRegistrationLoginLogout() throws Exception {
        assertEquals(5, jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL", Integer.class));
        Client client = new Client();
        var command = Map.of("fullName", "Registered User", "email", "register@example.test", "password", PASSWORD);
        client.mutate("POST", "/api/users", command, 201);
        var stored = users.findByEmailIgnoreCase("register@example.test").orElseThrow();
        assertEquals(Role.CUSTOMER, stored.getRole());
        assertNotEquals(PASSWORD, stored.getPasswordHash()); assertTrue(encoder.matches(PASSWORD, stored.getPasswordHash()));
        client.mutate("POST", "/api/users", command, 409);
        client.mutate("POST", "/api/auth/login", Map.of("email", stored.getEmail(), "password", "wrong"), 401);
        client.login(stored.getEmail());
        assertEquals(stored.getId().longValue(), client.get("/api/auth/me", 200).path("id").asLong());
        client.mutate("POST", "/api/auth/logout", Map.of(), 204);
        client.get("/api/auth/me", 401);
    }

    @Test void completeShipmentLifecyclePersistsHistoryAndProtectsOwnership() throws Exception {
        User sender = seed("sender", Role.CUSTOMER), admin = seed("admin", Role.ADMIN), courier = seed("courier", Role.COURIER);
        User other = seed("other", Role.CUSTOMER), otherCourier = seed("other-courier", Role.COURIER);
        Client customer = login(sender), management = login(admin), delivery = login(courier);
        Client outsider = login(other), wrongCourier = login(otherCourier);
        long id = customer.mutate("POST", "/api/shipments", shipment(sender.getId(), "Receiver"), 201).path("id").asLong();
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM parcels WHERE shipment_id = ?", Integer.class, id));
        outsider.get("/api/shipments/" + id, 403);
        customer.mutate("POST", "/api/shipments", shipment(other.getId(), "Forged"), 403);
        customer.mutate("PATCH", "/api/shipments/" + id + "/courier", Map.of("courierId", courier.getId()), 403);
        management.mutate("PATCH", "/api/shipments/" + id + "/courier", Map.of("courierId", courier.getId()), 204);
        management.mutate("PATCH", "/api/shipments/" + id + "/courier", Map.of("courierId", otherCourier.getId()), 409);
        assertEquals(1, delivery.get("/api/shipments/assigned", 200).path("totalElements").asInt());
        wrongCourier.mutate("PATCH", "/api/shipments/" + id + "/status", Map.of("status", "PICKED_UP"), 403);
        delivery.mutate("PATCH", "/api/shipments/" + id + "/status", Map.of("status", "DELIVERED"), 409);
        for (String status : List.of("PICKED_UP", "IN_TRANSIT", "DELIVERY_FAILED", "IN_TRANSIT", "DELIVERED")) {
            delivery.mutate("PATCH", "/api/shipments/" + id + "/status", Map.of("status", status), 204);
        }
        delivery.mutate("PATCH", "/api/shipments/" + id + "/status", Map.of("status", "IN_TRANSIT"), 409);
        JsonNode details = customer.get("/api/shipments/" + id, 200);
        assertEquals("DELIVERED", details.path("status").asText());
        assertEquals(10, details.path("trackingNumber").asText().length());
        JsonNode history = customer.get("/api/shipments/" + id + "/tracking-events", 200);
        assertEquals(7, history.size());
        assertEquals("REGISTERED", history.get(0).path("status").asText());
        assertEquals("DELIVERED", history.get(6).path("status").asText());
        for (int index = 1; index < history.size(); index++) {
            var previous = java.time.OffsetDateTime.parse(history.get(index-1).path("occurredAt").asText());
            var current = java.time.OffsetDateTime.parse(history.get(index).path("occurredAt").asText());
            assertFalse(current.isBefore(previous));
        }
        assertEquals(7, jdbc.queryForObject("SELECT count(*) FROM tracking_events WHERE shipment_id = ?", Integer.class, id));
    }

    @Test void pagingFilteringAndSavedAddressSnapshot() throws Exception {
        User sender = seed("sender", Role.CUSTOMER); Client customer = login(sender);
        var address = address("Saved street");
        customer.mutate("PATCH", "/api/users/"+sender.getId(), Map.of("fullName","New Name", "address", address), 200);
        assertEquals("Saved street", customer.get("/api/auth/me",200).path("address").path("street").asText());
        var first = shipment(sender.getId(),"First"); first.put("pickupAddress", address);
        long firstId = customer.mutate("POST","/api/shipments",first,201).path("id").asLong();
        long secondId = customer.mutate("POST","/api/shipments",shipment(sender.getId(),"Second"),201).path("id").asLong();
        var page = customer.get("/api/shipments/my?size=1&page=0",200);
        assertEquals(2,page.path("totalPages").asInt());
        assertEquals(secondId,page.path("content").get(0).path("id").asLong());
        assertEquals(firstId,customer.get("/api/shipments/my?size=1&page=1",200).path("content").get(0).path("id").asLong());
        assertEquals(0,customer.get("/api/shipments/my?status=DELIVERED",200).path("totalElements").asInt());
        assertEquals(2,customer.get("/api/shipments/my?status=REGISTERED",200).path("totalElements").asInt());
        customer.mutate("PATCH","/api/users/"+sender.getId(),Map.of("address",address("Changed street")),200);
        assertEquals("Saved street",customer.get("/api/shipments/"+firstId,200).path("pickupAddress").path("street").asText());
    }

    @Test void administratorChangesAccessAndDisabledSessionIsRejected() throws Exception {
        User admin = seed("admin",Role.ADMIN), target = seed("target",Role.CUSTOMER);
        Client management=login(admin), customer=login(target);
        management.mutate("PATCH","/api/admin/users/"+target.getId()+"/access",Map.of("role","COURIER","active",true),200);
        assertEquals(Role.COURIER,users.findById(target.getId()).orElseThrow().getRole());
        assertEquals(1,management.get("/api/admin/users?role=COURIER&active=true",200).path("totalElements").asInt());
        management.mutate("PATCH","/api/admin/users/"+admin.getId()+"/access",Map.of("role","CUSTOMER","active",true),409);
        management.mutate("PATCH","/api/admin/users/"+target.getId()+"/access",Map.of("role","COURIER","active",false),200);
        customer.get("/api/auth/me",401); customer.get("/api/shipments/my",403);
        new Client().mutate("POST","/api/auth/login",Map.of("email",target.getEmail(),"password",PASSWORD),401);
    }

    @Test void validationAndCsrfFailuresDoNotWritePartialData() throws Exception {
        User sender=seed("sender",Role.CUSTOMER); Client customer=login(sender);
        var invalid = shipment(sender.getId(),"Invalid weight");
        invalid.put("parcels", List.of(Map.of("weight",1.234,"fragile",false)));
        var failure=customer.mutate("POST","/api/shipments",invalid,400);
        assertEquals("VALIDATION_ERROR",failure.path("errorCode").asText());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM shipments",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM addresses",Integer.class));
        customer.send("POST","/api/shipments",shipment(sender.getId(),"No token"),null,null,403);
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM shipments",Integer.class));
    }

    @Test void databaseFailureRollsBackTheWholeShipmentTransaction() {
        User sender=seed("sender",Role.CUSTOMER);
        var address=new hu.kolozsvaripeter.parceltracker.dto.incoming.CreateAddressDto(
            "Hungary","1211","Budapest","Rollback street","1",null);
        var parcel=new hu.kolozsvaripeter.parceltracker.dto.incoming.CreateParcelDto(
            new java.math.BigDecimal("-1"),"Rejected by database constraint",false);
        var command=new hu.kolozsvaripeter.parceltracker.dto.incoming.CreateShipmentDto(
            sender.getId(),address,"Rollback receiver",address,List.of(parcel));
        // Deliberately bypass controller validation to test the real database transaction boundary.
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> shipments.createShipment(command,sender.getEmail()));
        for(String table:List.of("shipments","addresses","parcels","tracking_events")) {
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM "+table,Integer.class));
        }
    }

    @Test void simultaneousCourierAssignmentsProduceOneWinnerAndOneHistoryEntry() throws Exception {
        User sender=seed("sender",Role.CUSTOMER),admin=seed("admin",Role.ADMIN);
        User first=seed("first-courier",Role.COURIER),second=seed("second-courier",Role.COURIER);
        long id=login(sender).mutate("POST","/api/shipments",shipment(sender.getId(),"Race receiver"),201).path("id").asLong();
        var start=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            List<java.util.concurrent.Future<Integer>> attempts=new ArrayList<>();
            for(User courier:List.of(first,second)) {
                attempts.add(pool.submit(() -> {
                    start.await();
                    try {
                        shipments.assignCourier(id,new hu.kolozsvaripeter.parceltracker.dto.incoming.AssignCourierDto(courier.getId()),admin.getEmail());
                        return 204;
                    } catch(hu.kolozsvaripeter.parceltracker.exception.ShipmentStateConflictException conflict) {
                        return 409;
                    }
                }));
            }
            start.countDown();
            List<Integer> results=new ArrayList<>();
            for(var attempt:attempts)results.add(attempt.get(15,java.util.concurrent.TimeUnit.SECONDS));
            Collections.sort(results);assertEquals(List.of(204,409),results);
        }
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM tracking_events WHERE shipment_id = ?",Integer.class,id));
        assertEquals("COURIER_ASSIGNED",jdbc.queryForObject("SELECT status FROM shipments WHERE id = ?",String.class,id));
    }

    private User seed(String prefix,Role role) {
        return users.saveAndFlush(new User(prefix+"@example.test",encoder.encode(PASSWORD),prefix,null,role));
    }
    private Client login(User user) throws Exception { Client client=new Client(); client.login(user.getEmail()); return client; }
    private Map<String,Object> address(String street) {return Map.of("country","Hungary","postalCode","1211","city","Budapest","street",street,"houseNumber","1");}
    private Map<String,Object> shipment(Long senderId,String receiver) {
        Map<String,Object> command=new HashMap<>();command.put("senderId",senderId);command.put("receiverName",receiver);
        command.put("pickupAddress",address("Pickup street"));command.put("deliveryAddress",address("Delivery street"));
        command.put("parcels",List.of(Map.of("weight",1.25,"description","Books","fragile",false)));return command;
    }
    class Client {
        final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        void login(String email) throws Exception {mutate("POST","/api/auth/login",Map.of("email",email,"password",PASSWORD),204);}
        JsonNode get(String path,int status) throws Exception {return send("GET",path,null,null,null,status);}
        JsonNode mutate(String method,String path,Object body,int status) throws Exception {
            JsonNode csrf=get("/api/auth/csrf",200);
            return send(method,path,body,csrf.path("headerName").asText(),csrf.path("token").asText(),status);
        }
        JsonNode send(String method,String path,Object body,String header,String token,int status) throws Exception {
            var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).timeout(Duration.ofSeconds(20));
            if (header!=null)request.header(header,token);
            if (body==null)request.method(method,HttpRequest.BodyPublishers.noBody());
            else request.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
            var response=http.send(request.build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(status,response.statusCode(),()->method+" "+path+" returned "+response.body());
            return response.body().isBlank()?JSON.createObjectNode():JSON.readTree(response.body());
        }
    }
}
