package com.lpgchain.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.RoleSelectionRequestDTO;
import com.lpgchain.dto.UserRequestDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.RoleRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PrivilegeEscalationAndRebacSecurityTest {

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
    private RoleRepository roleRepository;

    @MockBean
    private DistributorRepository distributorRepository;

    @MockBean
    private DistributorMembershipRepository distributorMembershipRepository;

    @MockBean
    private BookingRepository bookingRepository;

    private User customerUserA;
    private User customerUserB;
    private User distributorUserOrg1;
    private User distributorUserOrg2;
    private User attackerWithAdminDomain;

    private Role customerRole;
    private Role distributorRole;
    private Role adminRole;

    private Distributor distributorOrg1;
    private Distributor distributorOrg2;

    private String customerAToken;
    private String distributorOrg1Token;
    private String adminDomainNoRoleToken;

    @BeforeEach
    void setUp() {
        customerRole = new Role(1L, "CUSTOMER");
        distributorRole = new Role(2L, "DISTRIBUTOR");
        adminRole = new Role(3L, "ADMIN");

        distributorOrg1 = new Distributor();
        distributorOrg1.setId(100L);
        distributorOrg1.setAgencyName("Distributor Org 1");

        distributorOrg2 = new Distributor();
        distributorOrg2.setId(200L);
        distributorOrg2.setAgencyName("Distributor Org 2");

        // 1. Customer User A
        customerUserA = new User(1L, "Customer A", "customera@gmail.com", "hash");
        when(userRepository.findByEmail("customera@gmail.com")).thenReturn(Optional.of(customerUserA));
        when(userRoleRepository.findByUser(customerUserA)).thenReturn(List.of(
                new UserRole(1L, customerUserA, customerRole)
        ));
        customerAToken = jwtTokenProvider.generateToken(1L, "customera@gmail.com", "CUSTOMER");

        // 2. Customer User B
        customerUserB = new User(2L, "Customer B", "customerb@gmail.com", "hash");
        when(userRepository.findByEmail("customerb@gmail.com")).thenReturn(Optional.of(customerUserB));
        when(userRoleRepository.findByUser(customerUserB)).thenReturn(List.of(
                new UserRole(2L, customerUserB, customerRole)
        ));

        // 3. Distributor User in Org 1
        distributorUserOrg1 = new User(10L, "Dist 1 User", "manager@approved-distributor.example", "hash");
        when(userRepository.findByEmail("manager@approved-distributor.example")).thenReturn(Optional.of(distributorUserOrg1));
        when(userRoleRepository.findByUser(distributorUserOrg1)).thenReturn(List.of(
                new UserRole(3L, distributorUserOrg1, distributorRole)
        ));
        distributorOrg1Token = jwtTokenProvider.generateToken(10L, "manager@approved-distributor.example", "DISTRIBUTOR", 100L);

        DistributorMembership membershipOrg1 = new DistributorMembership(1L, distributorUserOrg1, distributorOrg1, "MANAGER", "ACTIVE");
        when(distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(10L, "ACTIVE")).thenReturn(List.of(membershipOrg1));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(10L, 100L)).thenReturn(Optional.of(membershipOrg1));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(10L, 200L)).thenReturn(Optional.empty());

        // 4. Attacker possessing approved admin email domain, but NO admin role
        attackerWithAdminDomain = new User(99L, "Attacker User", "attacker@lpgchain.example", "hash");
        when(userRepository.findByEmail("attacker@lpgchain.example")).thenReturn(Optional.of(attackerWithAdminDomain));
        when(userRoleRepository.findByUser(attackerWithAdminDomain)).thenReturn(List.of(
                new UserRole(4L, attackerWithAdminDomain, customerRole) // only has customer role!
        ));
        adminDomainNoRoleToken = jwtTokenProvider.generateToken(99L, "attacker@lpgchain.example", "CUSTOMER");
    }

    // =========================================================================
    // 401 UNAUTHORIZED VS 403 FORBIDDEN EXPLICIT TESTS
    // =========================================================================
    @Test
    @DisplayName("401 VS 403: Unauthenticated request to protected endpoint returns 401 Unauthorized")
    void shouldReturn401ForUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    @DisplayName("401 VS 403: Authenticated user without required role returns 403 Forbidden")
    void shouldReturn403ForInsufficientRole() throws Exception {
        // Customer attempting to access Admin endpoint
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // Customer attempting to access Distributor endpoint
        mockMvc.perform(get("/api/distributor/test")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // PRIVILEGE ESCALATION PREVENTION TESTS
    // =========================================================================
    @Test
    @DisplayName("PRIVILEGE ESCALATION: Public registration rejecting self-assignment of ADMIN role")
    void shouldBlockPublicRegistrationSelfAssigningAdmin() throws Exception {
        UserRequestDTO request = new UserRequestDTO();
        request.setName("Hacker");
        request.setEmail("hacker@gmail.com");
        request.setPassword("Password@123");
        request.setRole("ADMIN");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("PRIVILEGE ESCALATION: Public registration rejecting self-assignment of DISTRIBUTOR role")
    void shouldBlockPublicRegistrationSelfAssigningDistributor() throws Exception {
        UserRequestDTO request = new UserRequestDTO();
        request.setName("Hacker");
        request.setEmail("hacker2@gmail.com");
        request.setPassword("Password@123");
        request.setRole("DISTRIBUTOR");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("PRIVILEGE ESCALATION: User cannot select ADMIN role if ADMIN is not assigned")
    void shouldRejectSelectingUnassignedAdminRole() throws Exception {
        RoleSelectionRequestDTO request = new RoleSelectionRequestDTO("ADMIN");

        mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("PRIVILEGE ESCALATION: Forged active role in JWT fails signature verification (401)")
    void shouldRejectForgedJwtRoleClaim() throws Exception {
        // Tamper with the token to alter payload
        String tamperedToken = customerAToken.substring(0, customerAToken.length() - 6) + "xyz123";

        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("PRIVILEGE ESCALATION: Official email domain alone does not grant role or access")
    void shouldNotAllowEmailDomainToGrantRole() throws Exception {
        // User has attacker@lpgchain.example (approved admin domain), but only has CUSTOMER role
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + adminDomainNoRoleToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // RELATIONSHIP-BASED ACCESS CONTROL (ReBAC) TESTS
    // =========================================================================
    @Test
    @DisplayName("ReBAC: Distributor user in Org 1 cannot access memberships of Org 2 (403 Forbidden)")
    void shouldRejectCrossDistributorAccess() throws Exception {
        // Manager in Org 1 requests memberships for Org 2
        mockMvc.perform(get("/api/memberships/distributor/200")
                        .header("Authorization", "Bearer " + distributorOrg1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("ReBAC: Customer A cannot access Customer B's bookings (403 Forbidden)")
    void shouldRejectCrossCustomerBookingAccess() throws Exception {
        // Customer A (ID 1) attempts to access bookings for Customer B (ID 2)
        mockMvc.perform(get("/api/bookings/customer/2")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
