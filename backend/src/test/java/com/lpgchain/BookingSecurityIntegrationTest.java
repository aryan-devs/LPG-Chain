package com.lpgchain;

import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserRoleRepository userRoleRepository;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private DistributorRepository distributorRepository;

    @MockBean
    private DistributorMembershipRepository distributorMembershipRepository;

    private String customerAToken;
    private String customerBToken;
    private String distributorAToken;
    private String distributorBToken;
    private String adminToken;

    private User customerA;
    private User customerB;
    private User distributorUserA;
    private User distributorUserB;
    private User adminUser;

    private Distributor distributorOrgA;
    private Distributor distributorOrgB;

    private Booking booking1; // Owned by Customer A, assigned to Distributor A, PENDING
    private Booking booking2; // Owned by Customer B, assigned to Distributor B, PENDING
    private Booking booking3; // Owned by Customer A, assigned to Distributor A, DELIVERED

    @BeforeEach
    void setUp() {
        // Roles
        Role customerRole = new Role(1L, "CUSTOMER");
        Role distributorRole = new Role(2L, "DISTRIBUTOR");
        Role adminRole = new Role(3L, "ADMIN");

        // Users
        customerA = new User(101L, "Customer A", "customerA@lpgchain.demo", "hash");
        customerB = new User(102L, "Customer B", "customerB@lpgchain.demo", "hash");
        distributorUserA = new User(201L, "Distributor User A", "distributorA@lpgchain.demo", "hash");
        distributorUserB = new User(202L, "Distributor User B", "distributorB@lpgchain.demo", "hash");
        adminUser = new User(301L, "Admin User", "admin@lpgchain.demo", "hash");

        // User lookup mocks
        when(userRepository.findByEmail(customerA.getEmail())).thenReturn(Optional.of(customerA));
        when(userRepository.findByEmail(customerB.getEmail())).thenReturn(Optional.of(customerB));
        when(userRepository.findByEmail(distributorUserA.getEmail())).thenReturn(Optional.of(distributorUserA));
        when(userRepository.findByEmail(distributorUserB.getEmail())).thenReturn(Optional.of(distributorUserB));
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

        // User Role mocks
        when(userRoleRepository.findByUser(customerA)).thenReturn(List.of(new UserRole(customerA, customerRole)));
        when(userRoleRepository.findByUser(customerB)).thenReturn(List.of(new UserRole(customerB, customerRole)));
        when(userRoleRepository.findByUser(distributorUserA)).thenReturn(List.of(new UserRole(distributorUserA, distributorRole)));
        when(userRoleRepository.findByUser(distributorUserB)).thenReturn(List.of(new UserRole(distributorUserB, distributorRole)));
        when(userRoleRepository.findByUser(adminUser)).thenReturn(List.of(new UserRole(adminUser, adminRole)));

        // Distributors
        distributorOrgA = new Distributor(10L, "Gas Agency A", "LIC-A", "111111", "Address A", "ACTIVE", "pass", LocalDateTime.now().plusDays(1), false);
        distributorOrgB = new Distributor(20L, "Gas Agency B", "LIC-B", "222222", "Address B", "ACTIVE", "pass", LocalDateTime.now().plusDays(1), false);

        // Distributor Memberships
        DistributorMembership membershipA = new DistributorMembership(1L, distributorUserA, distributorOrgA, "STAFF", "ACTIVE");
        DistributorMembership membershipB = new DistributorMembership(2L, distributorUserB, distributorOrgB, "STAFF", "ACTIVE");

        when(distributorMembershipRepository.existsByUserIdAndDistributorId(201L, 10L)).thenReturn(true);
        when(distributorMembershipRepository.existsByUserIdAndDistributorId(202L, 20L)).thenReturn(true);
        when(distributorMembershipRepository.findByUserId(201L)).thenReturn(List.of(membershipA));
        when(distributorMembershipRepository.findByUserId(202L)).thenReturn(List.of(membershipB));

        // Bookings
        booking1 = new Booking(1L, customerA, distributorOrgA, "Customer A Address", LocalDateTime.now(), 1, "PENDING");
        booking2 = new Booking(2L, customerB, distributorOrgB, "Customer B Address", LocalDateTime.now(), 1, "PENDING");
        booking3 = new Booking(3L, customerA, distributorOrgA, "Customer A Address", LocalDateTime.now().minusDays(2), 1, "DELIVERED");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking1));
        when(bookingRepository.findById(2L)).thenReturn(Optional.of(booking2));
        when(bookingRepository.findById(3L)).thenReturn(Optional.of(booking3));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Tokens
        customerAToken = jwtTokenProvider.generateToken(101L, "customerA@lpgchain.demo");
        customerBToken = jwtTokenProvider.generateToken(102L, "customerB@lpgchain.demo");
        distributorAToken = jwtTokenProvider.generateToken(201L, "distributorA@lpgchain.demo");
        distributorBToken = jwtTokenProvider.generateToken(202L, "distributorB@lpgchain.demo");
        adminToken = jwtTokenProvider.generateToken(301L, "admin@lpgchain.demo");
    }

    // =========================================================================
    // CUSTOMER SCENARIOS
    // =========================================================================

    @Test
    @DisplayName("Scenario 1: Customer A requests own booking -> 200 OK")
    void testCustomerARequestsOwnBooking() throws Exception {
        mockMvc.perform(get("/api/bookings/1")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerId").value(101));
    }

    @Test
    @DisplayName("Scenario 2: Customer A requests Customer B's booking -> 403 Forbidden")
    void testCustomerARequestsCustomerBBooking() throws Exception {
        mockMvc.perform(get("/api/bookings/2")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Access denied to booking #2"));
    }

    @Test
    @DisplayName("Scenario 3: Customer A attempts distributor delivery-status operation -> 403 Forbidden")
    void testCustomerAAttemptsDistributorDeliveryStatusOperation() throws Exception {
        mockMvc.perform(patch("/api/bookings/1/status")
                        .header("Authorization", "Bearer " + customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Customers cannot update delivery status"));
    }

    @Test
    @DisplayName("Customer A cancels own PENDING booking -> 200 OK")
    void testCustomerACancelsOwnBooking() throws Exception {
        mockMvc.perform(patch("/api/bookings/1/status")
                        .header("Authorization", "Bearer " + customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // =========================================================================
    // DISTRIBUTOR SCENARIOS
    // =========================================================================

    @Test
    @DisplayName("Scenario 4: Distributor A accesses booking assigned to Distributor A -> 200 OK")
    void testDistributorAAccessesAssignedBooking() throws Exception {
        mockMvc.perform(get("/api/bookings/1")
                        .header("Authorization", "Bearer " + distributorAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.distributorId").value(10));
    }

    @Test
    @DisplayName("Scenario 5: Distributor A accesses/updates booking assigned to Distributor B -> 403 Forbidden")
    void testDistributorAAccessesDistributorBBooking() throws Exception {
        // Read access denied
        mockMvc.perform(get("/api/bookings/2")
                        .header("Authorization", "Bearer " + distributorAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Update status denied
        mockMvc.perform(patch("/api/bookings/2/status")
                        .header("Authorization", "Bearer " + distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Booking is not assigned to your distributor organization"));
    }

    @Test
    @DisplayName("Scenario 6: Distributor A attempts invalid booking status transition -> 400 Bad Request")
    void testDistributorAAttemptsInvalidBookingStatusTransition() throws Exception {
        // Booking 1 is PENDING. Valid transitions from PENDING are CONFIRMED and CANCELLED.
        // Transition directly to DELIVERED is invalid.
        mockMvc.perform(patch("/api/bookings/1/status")
                        .header("Authorization", "Bearer " + distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Distributor A performs valid status transition PENDING -> CONFIRMED -> 200 OK")
    void testDistributorAPerformsValidTransition() throws Exception {
        mockMvc.perform(patch("/api/bookings/1/status")
                        .header("Authorization", "Bearer " + distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    // =========================================================================
    // ADMIN SCENARIOS
    // =========================================================================

    @Test
    @DisplayName("Scenario 7: Admin accesses authorized booking operation -> 200 OK")
    void testAdminAccessesBookingOperation() throws Exception {
        mockMvc.perform(get("/api/bookings/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        mockMvc.perform(get("/api/bookings/2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    // =========================================================================
    // AUTHENTICATION SCENARIOS
    // =========================================================================

    @Test
    @DisplayName("Scenario 8: No JWT -> 401 Unauthorized")
    void testNoJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/bookings/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    @DisplayName("Scenario 9: Invalid JWT -> 401 Unauthorized")
    void testInvalidJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/bookings/1")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // =========================================================================
    // ROLE / RBAC LEVEL RESTRICTIONS
    // =========================================================================

    @Test
    @DisplayName("Scenario 10: CUSTOMER accessing distributor-only operation -> 403 Forbidden")
    void testCustomerAccessingDistributorOperationReturns403() throws Exception {
        mockMvc.perform(get("/api/distributor/test")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/bookings/distributor/10")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Scenario 11: DISTRIBUTOR accessing admin-only operation -> 403 Forbidden")
    void testDistributorAccessingAdminOperationReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + distributorAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(delete("/api/bookings/1")
                        .header("Authorization", "Bearer " + distributorAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
