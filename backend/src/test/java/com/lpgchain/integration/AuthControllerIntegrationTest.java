package com.lpgchain.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.ComplaintRepository;
import com.lpgchain.repository.CustomerDocumentRepository;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.PaymentRepository;
import com.lpgchain.repository.RoleRepository;
import com.lpgchain.repository.SavedAddressRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.security.PasswordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthControllerIntegrationTest - Complete end-to-end integration tests for the LPG-Chain backend.
 *
 * Verifies the actual application pipeline without mocking repositories or services:
 *
 *   HTTP Request
 *     -> Spring Security Filter Chain (JwtAuthenticationFilter)
 *       -> Controller (@Valid, @RequestBody)
 *         -> Service (business logic, BCrypt+Pepper, EmailPolicy, RBAC, ReBAC, ABAC)
 *           -> Repository (JPA)
 *             -> Isolated H2 In-Memory Database (jdbc:h2:mem:lpgchaintest)
 *               -> HTTP Response (JSON via GlobalExceptionHandler / Security Entry Points)
 *
 * Test isolation strategy:
 *   - @BeforeEach seeds fresh, known test data into H2 for every test.
 *   - @AfterEach deletes all test data in the reverse FK dependency order.
 *   - Tests are independent and order-agnostic.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.DisplayName.class)
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordService passwordService;

    // Real repositories -- no @MockBean used in this integration suite
    @Autowired private UserRepository userRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private DistributorRepository distributorRepository;
    @Autowired private DistributorMembershipRepository distributorMembershipRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private DeliveryRepository deliveryRepository;
    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private CustomerDocumentRepository customerDocumentRepository;
    @Autowired private SavedAddressRepository savedAddressRepository;

    private User customerUserA;
    private User customerUserB;
    private User distributorUser;
    private User adminUser;
    private User multiRoleUser;
    private Distributor testDistributor;
    private Booking bookingOwnedByA;

    private Role customerRole;
    private Role distributorRole;
    private Role adminRole;

    private static final String RAW_PASSWORD = "Integration@Test99!";

    @BeforeEach
    void setUp() {
        customerRole    = roleRepository.findByName("CUSTOMER").orElseGet(() -> roleRepository.save(new Role("CUSTOMER")));
        distributorRole = roleRepository.findByName("DISTRIBUTOR").orElseGet(() -> roleRepository.save(new Role("DISTRIBUTOR")));
        adminRole       = roleRepository.findByName("ADMIN").orElseGet(() -> roleRepository.save(new Role("ADMIN")));

        String hashedPassword = passwordService.encode(RAW_PASSWORD);

        // Customer A (personal email -- allowed for CUSTOMER)
        customerUserA = userRepository.save(
                new User("Customer A", "customerA@integration.test", hashedPassword));
        userRoleRepository.save(new UserRole(customerUserA, customerRole));

        // Customer B (personal email -- allowed for CUSTOMER)
        customerUserB = userRepository.save(
                new User("Customer B", "customerB@integration.test", hashedPassword));
        userRoleRepository.save(new UserRole(customerUserB, customerRole));

        // Distributor organization
        testDistributor = distributorRepository.save(new Distributor(
                "Integration Gas Agency", "LIC-INT-001", "9000000001",
                "Test City", "ACTIVE", "testpasskey",
                LocalDateTime.now().plusDays(30), false));

        // Distributor User (lpgchain.demo = approved distributor domain) with active membership
        distributorUser = userRepository.save(
                new User("Dist Staff", "staff@lpgchain.demo", hashedPassword));
        userRoleRepository.save(new UserRole(distributorUser, distributorRole));
        distributorMembershipRepository.save(
                new DistributorMembership(distributorUser, testDistributor, "STAFF", "ACTIVE"));

        // Admin User (lpgchain.example = approved admin domain)
        adminUser = userRepository.save(
                new User("System Admin", "sysadmin@lpgchain.example", hashedPassword));
        userRoleRepository.save(new UserRole(adminUser, adminRole));

        // Multi-Role User (lpgchain.demo is whitelisted for both distributor + customer)
        multiRoleUser = userRepository.save(
                new User("Multi Role", "multi@lpgchain.demo", hashedPassword));
        userRoleRepository.save(new UserRole(multiRoleUser, customerRole));
        userRoleRepository.save(new UserRole(multiRoleUser, distributorRole));
        distributorMembershipRepository.save(
                new DistributorMembership(multiRoleUser, testDistributor, "STAFF", "ACTIVE"));

        // A booking owned by Customer A, assigned to the test distributor
        bookingOwnedByA = bookingRepository.save(new Booking(
                customerUserA, testDistributor,
                "123 Integration Street", LocalDateTime.now(), 2, "PENDING"));
    }

    @AfterEach
    void tearDown() {
        deliveryRepository.deleteAll();
        paymentRepository.deleteAll();
        complaintRepository.deleteAll();
        customerDocumentRepository.deleteAll();
        savedAddressRepository.deleteAll();
        bookingRepository.deleteAll();
        distributorMembershipRepository.deleteAll();
        userRoleRepository.deleteAll();
        userRepository.deleteAll();
        distributorRepository.deleteAll();
    }

    // =========================================================================
    // GROUP 1: AUTHENTICATION INTEGRATION
    // =========================================================================

    @Test
    @DisplayName("AUTH-01: Valid customer login returns 200 OK with JWT and correct response fields")
    void shouldLoginSuccessfullyThroughRealHttpFlow() throws Exception {
        String loginJson = "{\"email\":\"customerA@integration.test\",\"password\":\"" + RAW_PASSWORD + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customerA@integration.test"))
                .andExpect(jsonPath("$.activeRole").value("CUSTOMER"))
                .andExpect(jsonPath("$.roleSelectionRequired").value(false))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    @DisplayName("AUTH-02: Login with incorrect password returns 401 Unauthorized")
    void shouldRejectInvalidPasswordThroughHttp() throws Exception {
        String loginJson = "{\"email\":\"customerA@integration.test\",\"password\":\"WrongPassword!000\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("AUTH-03: Login with unknown email returns 401 Unauthorized")
    void shouldReturn401ForUnknownUserLogin() throws Exception {
        String loginJson = "{\"email\":\"ghost@nowhere.com\",\"password\":\"SomePassword@1\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("AUTH-04: Login with blank email and password returns 400 Bad Request with validation errors map")
    void shouldReturn400ForBlankEmailAndPassword() throws Exception {
        String loginJson = "{\"email\":\"\",\"password\":\"\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isMap());
    }

    @Test
    @DisplayName("AUTH-05: Login with missing password field returns 400 Bad Request")
    void shouldReturn400ForMissingPasswordField() throws Exception {
        String loginJson = "{\"email\":\"customerA@integration.test\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("AUTH-06: Login response never exposes the hashed password field")
    void shouldNotReturnPasswordInLoginResponse() throws Exception {
        String loginJson = "{\"email\":\"customerA@integration.test\",\"password\":\"" + RAW_PASSWORD + "\"}";

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains("password"),
                "Login response must NOT contain any 'password' field");
    }

    // =========================================================================
    // GROUP 2: JWT END-TO-END
    // =========================================================================

    @Test
    @DisplayName("JWT-01: Accessing protected endpoints without a JWT returns 401 Unauthorized")
    void shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(get("/api/customer/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        mockMvc.perform(get("/api/distributor/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("JWT-02: Token extracted from real login response grants access to a protected endpoint")
    void shouldAllowAccessWithValidJwtOnProtectedEndpoint() throws Exception {
        String jwt = loginAndExtractToken("customerA@integration.test", RAW_PASSWORD);

        assertNotNull(jwt, "JWT must be present in login response");
        assertFalse(jwt.isBlank(), "JWT must not be blank");

        mockMvc.perform(get("/api/customer/test")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Customer endpoint accessed successfully"));
    }

    @Test
    @DisplayName("JWT-03: Malformed or invalid JWT returns 401 Unauthorized")
    void shouldReturn401ForInvalidJwtToken() throws Exception {
        mockMvc.perform(get("/api/customer/test")
                        .header("Authorization", "Bearer this.is.not.a.real.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.fake.payload"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // =========================================================================
    // GROUP 3: RBAC & 401 VS 403 DISTINCTION
    // =========================================================================

    @Test
    @DisplayName("RBAC-01: CUSTOMER token allowed on /api/customer/test (200) but denied /api/admin/test and /api/distributor/test (403)")
    void shouldEnforceCustomerRbacBoundaries() throws Exception {
        String jwt = loginAndExtractToken("customerA@integration.test", RAW_PASSWORD);

        mockMvc.perform(get("/api/customer/test")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Access denied"));

        mockMvc.perform(get("/api/distributor/test")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("RBAC-02: DISTRIBUTOR token allowed on /api/distributor/test (200) but denied /api/admin/test and /api/customer/test (403)")
    void shouldEnforceDistributorRbacBoundaries() throws Exception {
        String jwt = loginAndExtractToken("staff@lpgchain.demo", RAW_PASSWORD);

        mockMvc.perform(get("/api/distributor/test")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/customer/test")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("RBAC-03: ADMIN token with official admin domain accesses /api/admin/test (200) but denied /api/customer and /api/distributor (403)")
    void shouldAllowAdminAccessToAdminEndpoint() throws Exception {
        String jwt = loginAndExtractToken("sysadmin@lpgchain.example", RAW_PASSWORD);

        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Admin endpoint accessed successfully"));

        mockMvc.perform(get("/api/customer/test")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/distributor/test")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC-04: Explicitly verifies 401 Unauthorized (No JWT) vs 403 Forbidden (Valid JWT + Insufficient Role)")
    void shouldDistinguishBetween401UnauthorizedAnd403Forbidden() throws Exception {
        // 1. No JWT -> 401 Unauthorized
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // 2. Valid JWT + insufficient role -> 403 Forbidden
        String customerJwt = loginAndExtractToken("customerA@integration.test", RAW_PASSWORD);
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + customerJwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // 3. Valid JWT + correct role -> 200 OK
        String adminJwt = loginAndExtractToken("sysadmin@lpgchain.example", RAW_PASSWORD);
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    // =========================================================================
    // GROUP 4: MULTI-ROLE AUTHENTICATION & ROLE SELECTION
    // =========================================================================

    @Test
    @DisplayName("MULTI-01: Multi-role user login returns roleSelectionRequired=true with all assigned roles")
    void shouldRequireRoleSelectionForMultiRoleUser() throws Exception {
        String loginJson = "{\"email\":\"multi@lpgchain.demo\",\"password\":\"" + RAW_PASSWORD + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("multi@lpgchain.demo"))
                .andExpect(jsonPath("$.roleSelectionRequired").value(true))
                .andExpect(jsonPath("$.activeRole").isEmpty())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Multiple roles available. Select a role."));
    }

    @Test
    @DisplayName("MULTI-02: Multi-role user selects CUSTOMER -> gets scoped token -> allowed on /api/customer/test (200), denied on /api/distributor/test (403)")
    void shouldSelectCustomerRoleFromMultiRoleUser() throws Exception {
        String loginJson = "{\"email\":\"multi@lpgchain.demo\",\"password\":\"" + RAW_PASSWORD + "\"}";

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String unscopedToken = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()).get("token").asText();

        MvcResult selectResult = mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + unscopedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("CUSTOMER"))
                .andExpect(jsonPath("$.roleSelectionRequired").value(false))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String scopedToken = objectMapper.readTree(
                selectResult.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/customer/test")
                        .header("Authorization", "Bearer " + scopedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        mockMvc.perform(get("/api/distributor/test")
                        .header("Authorization", "Bearer " + scopedToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("MULTI-03: Multi-role user selects DISTRIBUTOR -> gets scoped token -> allowed on /api/distributor/test (200), denied on /api/customer/test (403)")
    void shouldSelectDistributorRoleFromMultiRoleUser() throws Exception {
        String loginJson = "{\"email\":\"multi@lpgchain.demo\",\"password\":\"" + RAW_PASSWORD + "\"}";

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String unscopedToken = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()).get("token").asText();

        MvcResult selectResult = mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + unscopedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"DISTRIBUTOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("DISTRIBUTOR"))
                .andExpect(jsonPath("$.roleSelectionRequired").value(false))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String scopedToken = objectMapper.readTree(
                selectResult.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(get("/api/distributor/test")
                        .header("Authorization", "Bearer " + scopedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        mockMvc.perform(get("/api/customer/test")
                        .header("Authorization", "Bearer " + scopedToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("MULTI-04: Privilege escalation -- selecting ADMIN with only CUSTOMER+DISTRIBUTOR roles returns 403 Forbidden")
    void shouldRejectUnauthorizedRoleSelection() throws Exception {
        String loginJson = "{\"email\":\"multi@lpgchain.demo\",\"password\":\"" + RAW_PASSWORD + "\"}";

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String unscopedToken = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + unscopedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Role ADMIN is not assigned to user"));
    }

    // =========================================================================
    // GROUP 5: DISTRIBUTOR MEMBERSHIP & CROSS-ORGANIZATION ReBAC
    // =========================================================================

    @Test
    @DisplayName("REBAC-01: User in Distributor A cannot access bookings or resources of Distributor B (403 Forbidden)")
    void shouldRejectCrossDistributorAccess() throws Exception {
        // Create second distributor organization
        Distributor distributorB = distributorRepository.save(new Distributor(
                "Agency B", "LIC-INT-002", "9000000002",
                "City B", "ACTIVE", "passkeyB",
                LocalDateTime.now().plusDays(30), false));

        User distributorUserB = userRepository.save(
                new User("Dist Staff B", "staffB@lpgchain.demo", passwordService.encode(RAW_PASSWORD)));
        userRoleRepository.save(new UserRole(distributorUserB, distributorRole));
        DistributorMembership membershipB = distributorMembershipRepository.save(
                new DistributorMembership(distributorUserB, distributorB, "STAFF", "ACTIVE"));

        // Login as distributorUser (member of testDistributor / Distributor A)
        String jwtUserA = loginAndExtractToken("staff@lpgchain.demo", RAW_PASSWORD);

        // Attempt to access Distributor B's bookings -> 403 Forbidden
        mockMvc.perform(get("/api/bookings/distributor/" + distributorB.getId())
                        .header("Authorization", "Bearer " + jwtUserA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Attempt to access membership of User B in Distributor B -> 403 Forbidden
        mockMvc.perform(get("/api/memberships/" + membershipB.getId())
                        .header("Authorization", "Bearer " + jwtUserA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Can access own organization bookings -> 200 OK
        mockMvc.perform(get("/api/bookings/distributor/" + testDistributor.getId())
                        .header("Authorization", "Bearer " + jwtUserA))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // GROUP 6: BOOKING API & AUTHORIZATION FLOWS
    // =========================================================================

    @Test
    @DisplayName("BOOKING-01: Authenticated CUSTOMER creates a booking (201 Created) with default status PENDING")
    void shouldCreateBookingThroughHttp() throws Exception {
        String jwt = loginAndExtractToken("customerA@integration.test", RAW_PASSWORD);

        String bookingJson = "{\"customerId\":" + customerUserA.getId()
                + ",\"deliveryAddress\":\"456 Real H2 Street\",\"quantity\":3}";

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerUserA.getId()))
                .andExpect(jsonPath("$.deliveryAddress").value("456 Real H2 Street"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        long createdId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertTrue(bookingRepository.findById(createdId).isPresent(),
                "Booking must be persisted in H2 database");
    }

    @Test
    @DisplayName("BOOKING-02: Creating booking with missing required fields returns 400 Bad Request with validation errors")
    void shouldRejectBookingCreationWithInvalidData() throws Exception {
        String jwt = loginAndExtractToken("customerA@integration.test", RAW_PASSWORD);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isMap());
    }

    @Test
    @DisplayName("BOOKING-03: Customer B accessing Customer A booking returns 403 Forbidden (ReBAC ownership check)")
    void shouldRejectUnauthorizedBookingAccess() throws Exception {
        String jwtCustomerB = loginAndExtractToken("customerB@integration.test", RAW_PASSWORD);

        long bookingId = bookingOwnedByA.getId();

        mockMvc.perform(get("/api/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + jwtCustomerB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Access denied to booking #" + bookingId));
    }

    @Test
    @DisplayName("BOOKING-04: Owner (Customer A) can access their own booking (200 OK -- ReBAC)")
    void shouldAllowOwnerToAccessOwnBooking() throws Exception {
        String jwtCustomerA = loginAndExtractToken("customerA@integration.test", RAW_PASSWORD);

        long bookingId = bookingOwnedByA.getId();

        mockMvc.perform(get("/api/bookings/" + bookingId)
                        .header("Authorization", "Bearer " + jwtCustomerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.customerId").value(customerUserA.getId()));
    }

    @Test
    @DisplayName("BOOKING-05: Assigned distributor updates status PENDING -> CONFIRMED (200 OK)")
    void shouldUpdateBookingStatusFromPendingToConfirmed() throws Exception {
        String jwtDistributor = loginAndExtractToken("staff@lpgchain.demo", RAW_PASSWORD);

        long bookingId = bookingOwnedByA.getId();

        mockMvc.perform(patch("/api/bookings/" + bookingId + "/status")
                        .header("Authorization", "Bearer " + jwtDistributor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Verify status is updated in H2 database
        Booking updated = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals("CONFIRMED", updated.getStatus());
    }

    @Test
    @DisplayName("BOOKING-06: PENDING -> DELIVERED is an invalid status transition and returns 400 Bad Request (ABAC state machine)")
    void shouldRejectInvalidBookingStatusTransition() throws Exception {
        String jwtDistributor = loginAndExtractToken("staff@lpgchain.demo", RAW_PASSWORD);

        long bookingId = bookingOwnedByA.getId();

        // PENDING -> DELIVERED skips CONFIRMED; not a valid ABAC transition
        mockMvc.perform(patch("/api/bookings/" + bookingId + "/status")
                        .header("Authorization", "Bearer " + jwtDistributor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isBadRequest());

        // Verify database state was NOT changed
        Booking unchanged = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals("PENDING", unchanged.getStatus());
    }

    @Test
    @DisplayName("BOOKING-07: Customer attempting delivery-status update (CONFIRMED) returns 403 Forbidden (RBAC)")
    void shouldRejectCustomerAttemptingDeliveryStatusUpdate() throws Exception {
        String jwtCustomerA = loginAndExtractToken("customerA@integration.test", RAW_PASSWORD);

        long bookingId = bookingOwnedByA.getId();

        mockMvc.perform(patch("/api/bookings/" + bookingId + "/status")
                        .header("Authorization", "Bearer " + jwtCustomerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Customers cannot update delivery status"));
    }

    @Test
    @DisplayName("BOOKING-08: Customer can cancel their own PENDING booking (200 OK -- ABAC allowed transition)")
    void shouldAllowCustomerToCancelOwnPendingBooking() throws Exception {
        String jwtCustomerA = loginAndExtractToken("customerA@integration.test", RAW_PASSWORD);

        long bookingId = bookingOwnedByA.getId();

        mockMvc.perform(patch("/api/bookings/" + bookingId + "/status")
                        .header("Authorization", "Bearer " + jwtCustomerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("BOOKING-09: Unauthenticated booking access returns 401 Unauthorized")
    void shouldReturn401ForUnauthenticatedBookingAccess() throws Exception {
        long bookingId = bookingOwnedByA.getId();

        mockMvc.perform(get("/api/bookings/" + bookingId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    // =========================================================================
    // GROUP 7: GLOBAL EXCEPTION HANDLER
    // =========================================================================

    @Test
    @DisplayName("EXCEPTION-01: Validation failure returns structured {status, message, errors} JSON body")
    void shouldReturnValidationErrorStructure() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isMap())
                .andReturn();

        JsonNode errors = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("errors");

        assertNotNull(errors, "Validation errors map must be present in response body");
        assertTrue(errors.size() > 0, "At least one field-level validation error must be returned");
    }

    @Test
    @DisplayName("EXCEPTION-02: Unauthenticated request returns standard {status, message} 401 error structure")
    void shouldReturnStandardUnauthorizedStructure() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    @DisplayName("EXCEPTION-03: Unauthorized role access returns standard {status, message} 403 error structure")
    void shouldReturnStandardForbiddenStructure() throws Exception {
        String jwt = loginAndExtractToken("customerA@integration.test", RAW_PASSWORD);

        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Access denied"));
    }

    // =========================================================================
    // GROUP 8: OFFICIAL EMAIL POLICY INTEGRATION
    // =========================================================================

    @Test
    @DisplayName("POLICY-01: Customer registration with personal email is permitted")
    void shouldAllowCustomerRegistrationWithPersonalEmail() throws Exception {
        String registerJson = "{\"name\":\"Normal Customer\",\"email\":\"normal.customer@gmail.com\",\"password\":\"Pass@12345\"}";

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("normal.customer@gmail.com"));
    }

    @Test
    @DisplayName("POLICY-02: Distributor with approved official domain logs in successfully (200 OK)")
    void shouldAllowDistributorLoginWithApprovedOfficialDomain() throws Exception {
        String loginJson = "{\"email\":\"staff@lpgchain.demo\",\"password\":\"" + RAW_PASSWORD + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("DISTRIBUTOR"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("POLICY-03: Distributor with personal email is rejected under distributor context (400 Bad Request)")
    void shouldRejectDistributorWithPersonalEmailUnderDistributorContext() throws Exception {
        // Create a distributor user with a personal (non-approved) email domain
        User personalDist = userRepository.save(
                new User("Personal Dist", "dist.personal@gmail.com", passwordService.encode(RAW_PASSWORD)));
        userRoleRepository.save(new UserRole(personalDist, distributorRole));

        String loginJson = "{\"email\":\"dist.personal@gmail.com\",\"password\":\"" + RAW_PASSWORD + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Distributor accounts must use an approved official organization email address."));
    }

    @Test
    @DisplayName("POLICY-04: Admin with approved official domain logs in successfully (200 OK)")
    void shouldAllowAdminLoginWithApprovedOfficialDomain() throws Exception {
        String loginJson = "{\"email\":\"sysadmin@lpgchain.example\",\"password\":\"" + RAW_PASSWORD + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("ADMIN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("POLICY-05: Official email domain alone does NOT grant a role without database assignment")
    void shouldNotGrantAdminPrivilegeByDomainAloneWithoutDatabaseRole() throws Exception {
        // User with official domain @lpgchain.example but only CUSTOMER role in DB
        User impostor = userRepository.save(
                new User("Impostor", "employee@lpgchain.example", passwordService.encode(RAW_PASSWORD)));
        userRoleRepository.save(new UserRole(impostor, customerRole));

        String token = loginAndExtractToken("employee@lpgchain.example", RAW_PASSWORD);

        // Attempt to access ADMIN endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // GROUP 9: PRIVILEGE ESCALATION PREVENTION
    // =========================================================================

    @Test
    @DisplayName("ESCALATION-01: Public registration cannot self-assign ADMIN role (403 Forbidden)")
    void shouldRejectPublicRegistrationAttemptingToSelfAssignAdminRole() throws Exception {
        String requestJson = "{\"name\":\"Attacker\",\"email\":\"attacker@test.com\",\"password\":\"Test@12345\",\"role\":\"ADMIN\"}";

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Public registration does not permit self-assigning privileged roles (ADMIN)"));
    }

    @Test
    @DisplayName("ESCALATION-02: Public registration cannot self-assign DISTRIBUTOR role (403 Forbidden)")
    void shouldRejectPublicRegistrationAttemptingToSelfAssignDistributorRole() throws Exception {
        String requestJson = "{\"name\":\"Attacker Dist\",\"email\":\"attacker.dist@test.com\",\"password\":\"Test@12345\",\"role\":\"DISTRIBUTOR\"}";

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Public registration does not permit self-assigning privileged roles (DISTRIBUTOR)"));
    }

    @Test
    @DisplayName("ESCALATION-03: Customer cannot create a booking on behalf of another customer (403 Forbidden)")
    void shouldPreventCustomerFromCreatingBookingOnBehalfOfAnotherCustomer() throws Exception {
        String jwtCustomerA = loginAndExtractToken("customerA@integration.test", RAW_PASSWORD);

        // Customer A submits customerUserB's ID
        String bookingJson = "{\"customerId\":" + customerUserB.getId()
                + ",\"deliveryAddress\":\"Malicious Address\",\"quantity\":1}";

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtCustomerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Cannot create a booking on behalf of another customer"));
    }

    // =========================================================================
    // GROUP 10: UNIFIED AUTH & CUSTOMER REGISTRATION INTEGRATION
    // =========================================================================

    @Test
    @DisplayName("AUTH-REG-01: Public customer registration succeeds via /api/auth/register and allows immediate login")
    void shouldRegisterCustomerViaAuthEndpointAndLogin() throws Exception {
        String registerJson = "{\"name\":\"New Customer\",\"email\":\"new.customer@gmail.com\",\"password\":\"Pass@12345\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.customer@gmail.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        // Validate immediate login works with registered credentials
        String loginJson = "{\"email\":\"new.customer@gmail.com\",\"password\":\"Pass@12345\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("CUSTOMER"))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    @DisplayName("AUTH-REG-02: Duplicate customer registration is rejected with 400 Bad Request")
    void shouldRejectDuplicateCustomerRegistrationViaAuthEndpoint() throws Exception {
        String registerJson = "{\"name\":\"Customer A Duplicate\",\"email\":\"customerA@integration.test\",\"password\":\"Pass@12345\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("AUTH-REG-03: Public registration attempting ADMIN role is rejected with 403 Forbidden")
    void shouldRejectAdminRoleInPublicRegistrationViaAuthEndpoint() throws Exception {
        String registerJson = "{\"name\":\"Hacker Admin\",\"email\":\"hacker@gmail.com\",\"password\":\"Pass@12345\",\"role\":\"ADMIN\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("AUTH-REG-04: Public registration attempting DISTRIBUTOR role is rejected with 403 Forbidden")
    void shouldRejectDistributorRoleInPublicRegistrationViaAuthEndpoint() throws Exception {
        String registerJson = "{\"name\":\"Hacker Dist\",\"email\":\"hacker@agency.example\",\"password\":\"Pass@12345\",\"role\":\"DISTRIBUTOR\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("AUTH-ROLE-01: User with no assigned roles is rejected during login with 403 Forbidden")
    void shouldRejectLoginForUserWithNoAssignedRoles() throws Exception {
        User noRoleUser = userRepository.save(
                new User("Orphan User", "orphan@gmail.com", passwordService.encode(RAW_PASSWORD)));

        String loginJson = "{\"email\":\"orphan@gmail.com\",\"password\":\"" + RAW_PASSWORD + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: User account has no assigned roles. Please contact administrator."));
    }

    // =========================================================================
    // Helper: Perform real login and extract JWT from the response body
    // =========================================================================

    /**
     * Performs a real HTTP POST /api/auth/login using the Spring Security + BCrypt + Pepper + JWT
     * pipeline and extracts the JWT from the response. No tokens are hardcoded or pre-built.
     * Passwords and tokens are never printed to test logs.
     *
     * @param email       the user's email (must exist in H2 after @BeforeEach)
     * @param rawPassword the user's raw (non-hashed) password
     * @return the JWT token string from the login response
     */
    private String loginAndExtractToken(String email, String rawPassword) throws Exception {
        String loginJson = "{\"email\":\"" + email + "\",\"password\":\"" + rawPassword + "\"}";

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = responseJson.get("token").asText();

        assertNotNull(token, "Token must be present for user: " + email);
        assertFalse(token.isBlank(), "Token must not be blank for user: " + email);

        return token;
    }
}