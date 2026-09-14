package com.lpgchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.DistributorMembershipRequestDTO;
import com.lpgchain.dto.DistributorSelectionRequestDTO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DistributorMembershipSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserRoleRepository userRoleRepository;

    @MockBean
    private DistributorRepository distributorRepository;

    @MockBean
    private DistributorMembershipRepository distributorMembershipRepository;

    @MockBean
    private BookingRepository bookingRepository;

    private User distributorUserA;
    private User distributorUserB;
    private User multiDistUser;
    private User noMemberUser;
    private User inactiveMemberUser;
    private User dualRoleUser;
    private User ownerUserA;
    private User managerUserA;
    private User staffUserA;
    private User adminUser;
    private User customerUser;
    private User targetUser;

    private Distributor distributorOrgA;
    private Distributor distributorOrgB;

    private DistributorMembership membershipA;
    private DistributorMembership membershipB;
    private DistributorMembership multiMembershipA;
    private DistributorMembership multiMembershipB;
    private DistributorMembership inactiveMembershipA;
    private DistributorMembership dualMembershipA;
    private DistributorMembership ownerMembershipA;
    private DistributorMembership managerMembershipA;
    private DistributorMembership staffMembershipA;

    private Role customerRole;
    private Role distributorRole;
    private Role adminRole;

    private Booking bookingA;
    private Booking bookingB;

    @BeforeEach
    void setUp() {
        customerRole = new Role(1L, "CUSTOMER");
        distributorRole = new Role(2L, "DISTRIBUTOR");
        adminRole = new Role(3L, "ADMIN");

        customerUser = new User(101L, "Customer One", "customer@lpgchain.demo", "pass");
        distributorUserA = new User(201L, "Distributor A User", "distA@lpgchain.demo", "pass");
        distributorUserB = new User(202L, "Distributor B User", "distB@lpgchain.demo", "pass");
        multiDistUser = new User(203L, "Multi Dist User", "multiDist@lpgchain.demo", "pass");
        noMemberUser = new User(204L, "No Member User", "noMember@lpgchain.demo", "pass");
        inactiveMemberUser = new User(205L, "Inactive Member", "inactive@lpgchain.demo", "pass");
        dualRoleUser = new User(206L, "Dual Role User", "dual@lpgchain.demo", "pass");
        ownerUserA = new User(207L, "Owner A", "ownerA@lpgchain.demo", "pass");
        managerUserA = new User(208L, "Manager A", "managerA@lpgchain.demo", "pass");
        staffUserA = new User(209L, "Staff A", "staffA@lpgchain.demo", "pass");
        targetUser = new User(210L, "Target User", "target@lpgchain.demo", "pass");
        adminUser = new User(301L, "Admin User", "admin@lpgchain.demo", "pass");

        distributorOrgA = new Distributor(10L, "Gas Agency A", "LIC-A", "111111", "Pune", "ACTIVE", "pass", LocalDateTime.now().plusDays(1), false);
        distributorOrgB = new Distributor(20L, "Gas Agency B", "LIC-B", "222222", "Mumbai", "ACTIVE", "pass", LocalDateTime.now().plusDays(1), false);

        membershipA = new DistributorMembership(1L, distributorUserA, distributorOrgA, "STAFF", "ACTIVE");
        membershipB = new DistributorMembership(2L, distributorUserB, distributorOrgB, "STAFF", "ACTIVE");
        multiMembershipA = new DistributorMembership(3L, multiDistUser, distributorOrgA, "STAFF", "ACTIVE");
        multiMembershipB = new DistributorMembership(4L, multiDistUser, distributorOrgB, "STAFF", "ACTIVE");
        inactiveMembershipA = new DistributorMembership(5L, inactiveMemberUser, distributorOrgA, "STAFF", "INACTIVE");
        dualMembershipA = new DistributorMembership(6L, dualRoleUser, distributorOrgA, "STAFF", "ACTIVE");
        ownerMembershipA = new DistributorMembership(7L, ownerUserA, distributorOrgA, "OWNER", "ACTIVE");
        managerMembershipA = new DistributorMembership(8L, managerUserA, distributorOrgA, "MANAGER", "ACTIVE");
        staffMembershipA = new DistributorMembership(9L, staffUserA, distributorOrgA, "STAFF", "ACTIVE");

        bookingA = new Booking(1L, customerUser, distributorOrgA, "Pune", LocalDateTime.now(), 1, "PENDING");
        bookingB = new Booking(2L, customerUser, distributorOrgB, "Mumbai", LocalDateTime.now(), 1, "PENDING");

        // UserRepository stubs
        when(userRepository.findByEmail("customer@lpgchain.demo")).thenReturn(Optional.of(customerUser));
        when(userRepository.findByEmail("distA@lpgchain.demo")).thenReturn(Optional.of(distributorUserA));
        when(userRepository.findByEmail("distB@lpgchain.demo")).thenReturn(Optional.of(distributorUserB));
        when(userRepository.findByEmail("multiDist@lpgchain.demo")).thenReturn(Optional.of(multiDistUser));
        when(userRepository.findByEmail("noMember@lpgchain.demo")).thenReturn(Optional.of(noMemberUser));
        when(userRepository.findByEmail("inactive@lpgchain.demo")).thenReturn(Optional.of(inactiveMemberUser));
        when(userRepository.findByEmail("dual@lpgchain.demo")).thenReturn(Optional.of(dualRoleUser));
        when(userRepository.findByEmail("ownerA@lpgchain.demo")).thenReturn(Optional.of(ownerUserA));
        when(userRepository.findByEmail("managerA@lpgchain.demo")).thenReturn(Optional.of(managerUserA));
        when(userRepository.findByEmail("staffA@lpgchain.demo")).thenReturn(Optional.of(staffUserA));
        when(userRepository.findByEmail("admin@lpgchain.demo")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(210L)).thenReturn(Optional.of(targetUser));

        // UserRole stubs
        when(userRoleRepository.findByUser(distributorUserA)).thenReturn(List.of(new UserRole(distributorUserA, distributorRole)));
        when(userRoleRepository.findByUser(distributorUserB)).thenReturn(List.of(new UserRole(distributorUserB, distributorRole)));
        when(userRoleRepository.findByUser(multiDistUser)).thenReturn(List.of(new UserRole(multiDistUser, distributorRole)));
        when(userRoleRepository.findByUser(noMemberUser)).thenReturn(List.of(new UserRole(noMemberUser, distributorRole)));
        when(userRoleRepository.findByUser(inactiveMemberUser)).thenReturn(List.of(new UserRole(inactiveMemberUser, distributorRole)));
        when(userRoleRepository.findByUser(ownerUserA)).thenReturn(List.of(new UserRole(ownerUserA, distributorRole)));
        when(userRoleRepository.findByUser(managerUserA)).thenReturn(List.of(new UserRole(managerUserA, distributorRole)));
        when(userRoleRepository.findByUser(staffUserA)).thenReturn(List.of(new UserRole(staffUserA, distributorRole)));
        when(userRoleRepository.findByUser(customerUser)).thenReturn(List.of(new UserRole(customerUser, customerRole)));
        when(userRoleRepository.findByUser(adminUser)).thenReturn(List.of(new UserRole(adminUser, adminRole)));
        when(userRoleRepository.findByUser(dualRoleUser)).thenReturn(List.of(
                new UserRole(dualRoleUser, customerRole),
                new UserRole(dualRoleUser, distributorRole)
        ));

        // DistributorRepository stubs
        when(distributorRepository.findById(10L)).thenReturn(Optional.of(distributorOrgA));
        when(distributorRepository.findById(20L)).thenReturn(Optional.of(distributorOrgB));

        // DistributorMembershipRepository stubs
        when(distributorMembershipRepository.findByUserId(201L)).thenReturn(List.of(membershipA));
        when(distributorMembershipRepository.findByUserId(202L)).thenReturn(List.of(membershipB));
        when(distributorMembershipRepository.findByUserId(203L)).thenReturn(List.of(multiMembershipA, multiMembershipB));
        when(distributorMembershipRepository.findByUserId(204L)).thenReturn(List.of());
        when(distributorMembershipRepository.findByUserId(205L)).thenReturn(List.of(inactiveMembershipA));
        when(distributorMembershipRepository.findByUserId(206L)).thenReturn(List.of(dualMembershipA));
        when(distributorMembershipRepository.findByUserId(207L)).thenReturn(List.of(ownerMembershipA));
        when(distributorMembershipRepository.findByUserId(208L)).thenReturn(List.of(managerMembershipA));
        when(distributorMembershipRepository.findByUserId(209L)).thenReturn(List.of(staffMembershipA));

        when(distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(201L, "ACTIVE")).thenReturn(List.of(membershipA));
        when(distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(202L, "ACTIVE")).thenReturn(List.of(membershipB));
        when(distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(203L, "ACTIVE")).thenReturn(List.of(multiMembershipA, multiMembershipB));
        when(distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(204L, "ACTIVE")).thenReturn(List.of());
        when(distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(205L, "ACTIVE")).thenReturn(List.of());
        when(distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(206L, "ACTIVE")).thenReturn(List.of(dualMembershipA));
        when(distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(207L, "ACTIVE")).thenReturn(List.of(ownerMembershipA));
        when(distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(208L, "ACTIVE")).thenReturn(List.of(managerMembershipA));
        when(distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(209L, "ACTIVE")).thenReturn(List.of(staffMembershipA));

        when(distributorMembershipRepository.findByUserIdAndDistributorId(201L, 10L)).thenReturn(Optional.of(membershipA));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(201L, 20L)).thenReturn(Optional.empty());
        when(distributorMembershipRepository.findByUserIdAndDistributorId(202L, 20L)).thenReturn(Optional.of(membershipB));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(203L, 10L)).thenReturn(Optional.of(multiMembershipA));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(203L, 20L)).thenReturn(Optional.of(multiMembershipB));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(205L, 10L)).thenReturn(Optional.of(inactiveMembershipA));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(206L, 10L)).thenReturn(Optional.of(dualMembershipA));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(207L, 10L)).thenReturn(Optional.of(ownerMembershipA));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(208L, 10L)).thenReturn(Optional.of(managerMembershipA));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(209L, 10L)).thenReturn(Optional.of(staffMembershipA));

        when(distributorMembershipRepository.save(any(DistributorMembership.class))).thenAnswer(i -> {
            DistributorMembership m = i.getArgument(0);
            m.setId(99L);
            return m;
        });

        // BookingRepository stubs
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(bookingA));
        when(bookingRepository.findById(2L)).thenReturn(Optional.of(bookingB));
        when(bookingRepository.findByDistributorId(10L)).thenReturn(List.of(bookingA));
        when(bookingRepository.findByDistributorId(20L)).thenReturn(List.of(bookingB));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
    }

    // =========================================================================
    // TEST A — User with DISTRIBUTOR role + active membership to Distributor A
    // Expected: Distributor A resources -> allowed
    // =========================================================================
    @Test
    @DisplayName("TEST A: Distributor A member can access Distributor A bookings")
    void testA_distributorMemberAccessOwnDistributor() throws Exception {
        String token = jwtTokenProvider.generateToken(201L, "distA@lpgchain.demo", "DISTRIBUTOR", 10L);

        mockMvc.perform(get("/api/bookings/distributor/10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // TEST B — Same user tries Distributor B
    // Expected: 403 Forbidden
    // =========================================================================
    @Test
    @DisplayName("TEST B: Distributor A member accessing Distributor B resources is rejected with 403")
    void testB_distributorMemberCannotAccessOtherDistributor() throws Exception {
        String token = jwtTokenProvider.generateToken(201L, "distA@lpgchain.demo", "DISTRIBUTOR", 10L);

        mockMvc.perform(get("/api/bookings/distributor/20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // TEST C — User has DISTRIBUTOR role but no membership
    // Expected: 403 Forbidden for organization-specific distributor resources
    // =========================================================================
    @Test
    @DisplayName("TEST C: User with DISTRIBUTOR role but no membership is rejected with 403")
    void testC_distributorWithNoMembershipDenied() throws Exception {
        String token = jwtTokenProvider.generateToken(204L, "noMember@lpgchain.demo", "DISTRIBUTOR");

        mockMvc.perform(get("/api/bookings/distributor/10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // TEST D — User has membership but membership is INACTIVE
    // Expected: 403 Forbidden
    // =========================================================================
    @Test
    @DisplayName("TEST D: User with INACTIVE membership is rejected with 403")
    void testD_inactiveMembershipDenied() throws Exception {
        String token = jwtTokenProvider.generateToken(205L, "inactive@lpgchain.demo", "DISTRIBUTOR");

        mockMvc.perform(get("/api/bookings/distributor/10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // TEST E — User has CUSTOMER + DISTRIBUTOR roles
    // Select CUSTOMER -> Distributor APIs 403
    // Select DISTRIBUTOR -> Distributor membership context can be used
    // =========================================================================
    @Test
    @DisplayName("TEST E: Dual role user: CUSTOMER token denied distributor APIs (403); DISTRIBUTOR token allowed")
    void testE_dualRoleUserBehavior() throws Exception {
        // As CUSTOMER
        String customerToken = jwtTokenProvider.generateToken(206L, "dual@lpgchain.demo", "CUSTOMER");
        mockMvc.perform(get("/api/bookings/distributor/10")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // As DISTRIBUTOR
        String distributorToken = jwtTokenProvider.generateToken(206L, "dual@lpgchain.demo", "DISTRIBUTOR", 10L);
        mockMvc.perform(get("/api/bookings/distributor/10")
                        .header("Authorization", "Bearer " + distributorToken))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // TEST F — User belongs to two active distributors
    // Expected: Does not silently select one. Requires organization selection.
    // =========================================================================
    @Test
    @DisplayName("TEST F: User with multiple active distributors can select specific distributor context")
    void testF_multiDistributorSelection() throws Exception {
        String distToken = jwtTokenProvider.generateToken(203L, "multiDist@lpgchain.demo", "DISTRIBUTOR");

        // Select Distributor 10
        DistributorSelectionRequestDTO requestA = new DistributorSelectionRequestDTO(10L);
        mockMvc.perform(post("/api/auth/select-distributor")
                        .header("Authorization", "Bearer " + distToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distributorId").value(10))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Select Distributor 20
        DistributorSelectionRequestDTO requestB = new DistributorSelectionRequestDTO(20L);
        mockMvc.perform(post("/api/auth/select-distributor")
                        .header("Authorization", "Bearer " + distToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distributorId").value(20))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Attempt to select unassigned Distributor 99 -> 403 Forbidden
        DistributorSelectionRequestDTO requestInvalid = new DistributorSelectionRequestDTO(99L);
        mockMvc.perform(post("/api/auth/select-distributor")
                        .header("Authorization", "Bearer " + distToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalid)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // TEST G — OWNER creates/manages membership
    // Expected: 201 Created / allowed
    // =========================================================================
    @Test
    @DisplayName("TEST G: OWNER of Distributor A can create membership for target user")
    void testG_ownerCanCreateMembership() throws Exception {
        String ownerToken = jwtTokenProvider.generateToken(207L, "ownerA@lpgchain.demo", "DISTRIBUTOR", 10L);

        DistributorMembershipRequestDTO requestDTO = new DistributorMembershipRequestDTO(210L, 10L, "STAFF", "ACTIVE");

        mockMvc.perform(post("/api/memberships")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.distributorId").value(10))
                .andExpect(jsonPath("$.userId").value(210))
                .andExpect(jsonPath("$.membershipRole").value("STAFF"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // =========================================================================
    // TEST H — MANAGER attempts owner-only membership administration
    // Expected: 403 Forbidden
    // =========================================================================
    @Test
    @DisplayName("TEST H: MANAGER cannot create/manage memberships (403 Forbidden)")
    void testH_managerCannotManageMemberships() throws Exception {
        String managerToken = jwtTokenProvider.generateToken(208L, "managerA@lpgchain.demo", "DISTRIBUTOR", 10L);

        DistributorMembershipRequestDTO requestDTO = new DistributorMembershipRequestDTO(210L, 10L, "STAFF", "ACTIVE");

        mockMvc.perform(post("/api/memberships")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // TEST I — STAFF attempts owner-only membership administration
    // Expected: 403 Forbidden
    // =========================================================================
    @Test
    @DisplayName("TEST I: STAFF cannot create/manage memberships (403 Forbidden)")
    void testI_staffCannotManageMemberships() throws Exception {
        String staffToken = jwtTokenProvider.generateToken(209L, "staffA@lpgchain.demo", "DISTRIBUTOR", 10L);

        DistributorMembershipRequestDTO requestDTO = new DistributorMembershipRequestDTO(210L, 10L, "STAFF", "ACTIVE");

        mockMvc.perform(post("/api/memberships")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // TEST J — ADMIN performs authorized distributor-management operation
    // Expected: 201 Created / allowed
    // =========================================================================
    @Test
    @DisplayName("TEST J: ADMIN can create membership across any distributor organization")
    void testJ_adminCanManageMemberships() throws Exception {
        String adminToken = jwtTokenProvider.generateToken(301L, "admin@lpgchain.demo", "ADMIN");

        DistributorMembershipRequestDTO requestDTO = new DistributorMembershipRequestDTO(210L, 10L, "MANAGER", "ACTIVE");

        mockMvc.perform(post("/api/memberships")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.distributorId").value(10))
                .andExpect(jsonPath("$.membershipRole").value("MANAGER"));
    }

    // =========================================================================
    // TEST K — No JWT
    // Expected: 401 Unauthorized
    // =========================================================================
    @Test
    @DisplayName("TEST K: Accessing protected membership endpoint without JWT yields 401 Unauthorized")
    void testK_noJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/memberships/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(post("/api/auth/select-distributor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"distributorId\":10}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // =========================================================================
    // TEST L — Invalid JWT
    // Expected: 401 Unauthorized
    // =========================================================================
    @Test
    @DisplayName("TEST L: Accessing protected membership endpoint with invalid/malformed JWT yields 401 Unauthorized")
    void testL_invalidJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/memberships/my")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // =========================================================================
    // TEST M — Existing booking authorization still works
    // Customer ownership: owner allowed, other customer 403
    // Distributor assignment: correct distributor allowed, other distributor 403
    // ABAC status transitions: valid transition allowed, invalid transition 400
    // =========================================================================
    @Test
    @DisplayName("TEST M: Booking ReBAC + ABAC works: Customer owner allowed, other customer 403, valid transition 200, invalid transition 400")
    void testM_bookingRebacAbacCompatibility() throws Exception {
        String customerToken = jwtTokenProvider.generateToken(101L, "customer@lpgchain.demo", "CUSTOMER");
        String distAToken = jwtTokenProvider.generateToken(201L, "distA@lpgchain.demo", "DISTRIBUTOR", 10L);

        // 1. Customer can view own booking
        mockMvc.perform(get("/api/bookings/1")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        // 2. Assigned distributor can update status to CONFIRMED (valid ABAC transition)
        mockMvc.perform(patch("/api/bookings/1/status")
                        .header("Authorization", "Bearer " + distAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk());

        // 3. Invalid status transition (e.g. invalid status string) -> 400 Bad Request
        mockMvc.perform(patch("/api/bookings/1/status")
                        .header("Authorization", "Bearer " + distAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INVALID_STATUS_VALUE\"}"))
                .andExpect(status().isBadRequest());
    }
}
