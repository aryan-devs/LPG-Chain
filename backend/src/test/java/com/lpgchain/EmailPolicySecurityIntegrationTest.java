package com.lpgchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.RoleSelectionRequestDTO;
import com.lpgchain.dto.UserRequestDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.RoleRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.security.JwtTokenProvider;
import com.lpgchain.security.PasswordService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmailPolicySecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordService passwordService;

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

    private Role customerRole;
    private Role distributorRole;
    private Role adminRole;
    private Distributor distributorOrg;

    private final String rawPassword = "SecurePassword@123";
    private String hashedPassword;

    @BeforeEach
    void setUp() {
        hashedPassword = passwordService.encode(rawPassword);

        customerRole = new Role(1L, "CUSTOMER");
        distributorRole = new Role(2L, "DISTRIBUTOR");
        adminRole = new Role(3L, "ADMIN");

        distributorOrg = new Distributor();
        distributorOrg.setId(100L);
        distributorOrg.setAgencyName("Apex Gas Agency");

        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(roleRepository.findByName("DISTRIBUTOR")).thenReturn(Optional.of(distributorRole));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
    }

    // =========================================================================
    // SCENARIO 1: Customer email with personal domain (gmail.com) -> 200/201
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 1: Customer registration with personal domain (gmail.com) succeeds (201 Created)")
    void testScenario1_CustomerPersonalDomainGmail_Success() throws Exception {
        UserRequestDTO request = new UserRequestDTO();
        request.setName("Customer One");
        request.setEmail("customer@gmail.com");
        request.setPassword(rawPassword);
        request.setPhone("9876543210");
        request.setAddress("Mumbai");

        when(userRepository.findByEmail("customer@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User(1L, "Customer One", "customer@gmail.com", hashedPassword)));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("customer@gmail.com"));
    }

    // =========================================================================
    // SCENARIO 2: Customer email with personal domain (outlook.com) -> 200/201
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 2: Customer registration with personal domain (outlook.com) succeeds (201 Created)")
    void testScenario2_CustomerPersonalDomainOutlook_Success() throws Exception {
        UserRequestDTO request = new UserRequestDTO();
        request.setName("Customer Two");
        request.setEmail("customer@outlook.com");
        request.setPassword(rawPassword);
        request.setPhone("9876543211");
        request.setAddress("Delhi");

        when(userRepository.findByEmail("customer@outlook.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });
        when(userRepository.findById(2L)).thenReturn(Optional.of(new User(2L, "Customer Two", "customer@outlook.com", hashedPassword)));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("customer@outlook.com"));
    }

    // =========================================================================
    // SCENARIO 3: Distributor user with approved official domain -> 200 OK
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 3: Distributor user with approved official domain operates in DISTRIBUTOR context (200 OK)")
    void testScenario3_DistributorApprovedOfficialDomain_Success() throws Exception {
        String email = "manager@approved-distributor.example";
        User distUser = new User(30L, "Distributor Manager", email, hashedPassword);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(distUser));
        when(userRoleRepository.findByUser(distUser)).thenReturn(List.of(
                new UserRole(1L, distUser, distributorRole)
        ));

        DistributorMembership membership = new DistributorMembership(1L, distUser, distributorOrg, "MANAGER", "ACTIVE");
        when(distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(30L, "ACTIVE"))
                .thenReturn(List.of(membership));

        String loginPayload = "{\"email\":\"" + email + "\",\"password\":\"" + rawPassword + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("DISTRIBUTOR"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // =========================================================================
    // SCENARIO 4: Distributor user with personal domain -> 400 Bad Request
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 4: Distributor user with personal domain (gmail.com) is rejected with 400 Bad Request")
    void testScenario4_DistributorPersonalDomainGmail_Rejected() throws Exception {
        String email = "manager@gmail.com";
        User distUser = new User(40L, "Distributor Personal", email, hashedPassword);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(distUser));
        when(userRoleRepository.findByUser(distUser)).thenReturn(List.of(
                new UserRole(2L, distUser, distributorRole)
        ));

        String loginPayload = "{\"email\":\"" + email + "\",\"password\":\"" + rawPassword + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // =========================================================================
    // SCENARIO 5: Distributor user with unapproved domain -> 400 Bad Request
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 5: Distributor user with unapproved domain (attacker.com) is rejected with 400 Bad Request")
    void testScenario5_DistributorUnapprovedDomain_Rejected() throws Exception {
        String email = "manager@attacker.com";
        User distUser = new User(50L, "Attacker User", email, hashedPassword);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(distUser));
        when(userRoleRepository.findByUser(distUser)).thenReturn(List.of(
                new UserRole(3L, distUser, distributorRole)
        ));

        String loginPayload = "{\"email\":\"" + email + "\",\"password\":\"" + rawPassword + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // =========================================================================
    // SCENARIO 6: Distributor user with spoofed domain -> 400 Bad Request
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 6: Distributor user with spoofed subdomain suffix is rejected with 400 Bad Request")
    void testScenario6_DistributorSpoofedDomain_Rejected() throws Exception {
        String email = "manager@approved-distributor.example.attacker.com";
        User distUser = new User(60L, "Spoofer User", email, hashedPassword);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(distUser));
        when(userRoleRepository.findByUser(distUser)).thenReturn(List.of(
                new UserRole(4L, distUser, distributorRole)
        ));

        String loginPayload = "{\"email\":\"" + email + "\",\"password\":\"" + rawPassword + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // =========================================================================
    // SCENARIO 7: Admin user with approved official domain -> 200 OK
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 7: Admin user with approved official domain operates in ADMIN context (200 OK)")
    void testScenario7_AdminApprovedOfficialDomain_Success() throws Exception {
        String email = "admin@lpgchain.example";
        User adminUser = new User(70L, "System Admin", email, hashedPassword);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(adminUser));
        when(userRoleRepository.findByUser(adminUser)).thenReturn(List.of(
                new UserRole(5L, adminUser, adminRole)
        ));

        String loginPayload = "{\"email\":\"" + email + "\",\"password\":\"" + rawPassword + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("ADMIN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // =========================================================================
    // SCENARIO 8: Non-admin user with approved admin domain -> 403 Forbidden
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 8: User with admin domain but lacking ADMIN role cannot select ADMIN (403 Forbidden)")
    void testScenario8_NonAdminUserApprovedAdminDomain_Forbidden() throws Exception {
        // User has an official admin domain, but only has CUSTOMER role
        String email = "random@lpgchain.example";
        User randomUser = new User(80L, "Random Corp User", email, hashedPassword);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(randomUser));
        when(userRoleRepository.findByUser(randomUser)).thenReturn(List.of(
                new UserRole(6L, randomUser, customerRole)
        ));

        // Generate unscoped token for the user
        String unscopedToken = jwtTokenProvider.generateToken(80L, email);

        RoleSelectionRequestDTO selectRequest = new RoleSelectionRequestDTO("ADMIN");

        mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + unscopedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selectRequest)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // SCENARIO 9: Admin role attempted with personal domain -> 400 Bad Request
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 9: User with ADMIN role but personal email domain is rejected by policy (400 Bad Request)")
    void testScenario9_AdminRoleAttemptedWithPersonalDomain_Rejected() throws Exception {
        String email = "random@gmail.com";
        User adminPersonal = new User(90L, "Admin Personal", email, hashedPassword);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(adminPersonal));
        when(userRoleRepository.findByUser(adminPersonal)).thenReturn(List.of(
                new UserRole(7L, adminPersonal, adminRole)
        ));

        String loginPayload = "{\"email\":\"" + email + "\",\"password\":\"" + rawPassword + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // =========================================================================
    // SCENARIO 10: Registration endpoint rejecting role parameter for ADMIN
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 10: Public registration attempting role self-assignment of ADMIN is rejected (403 Forbidden)")
    void testScenario10_PublicRegistrationRejectsAdminRole_Forbidden() throws Exception {
        UserRequestDTO request = new UserRequestDTO();
        request.setName("Malicious User");
        request.setEmail("attacker1@gmail.com");
        request.setPassword(rawPassword);
        request.setRole("ADMIN");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // SCENARIO 11: Registration endpoint rejecting role parameter for DISTRIBUTOR
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 11: Public registration attempting role self-assignment of DISTRIBUTOR is rejected (403 Forbidden)")
    void testScenario11_PublicRegistrationRejectsDistributorRole_Forbidden() throws Exception {
        UserRequestDTO request = new UserRequestDTO();
        request.setName("Malicious User");
        request.setEmail("attacker2@gmail.com");
        request.setPassword(rawPassword);
        request.setRole("DISTRIBUTOR");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // SCENARIO 12: Dual-role user with personal email operating in CUSTOMER context -> 200 OK
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 12: Dual-role user with personal email selecting CUSTOMER context succeeds (200 OK)")
    void testScenario12_DualRoleUserInCustomerContext_Success() throws Exception {
        String email = "dual@gmail.com";
        User dualUser = new User(120L, "Dual Role User", email, hashedPassword);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(dualUser));
        when(userRoleRepository.findByUser(dualUser)).thenReturn(List.of(
                new UserRole(8L, dualUser, customerRole),
                new UserRole(9L, dualUser, distributorRole)
        ));

        String unscopedToken = jwtTokenProvider.generateToken(120L, email);

        RoleSelectionRequestDTO selectRequest = new RoleSelectionRequestDTO("CUSTOMER");

        mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + unscopedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("CUSTOMER"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // =========================================================================
    // SCENARIO 13: Dual-role user with personal email operating in DISTRIBUTOR context -> 400 Bad Request
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 13: Dual-role user with personal email selecting DISTRIBUTOR context is rejected (400 Bad Request)")
    void testScenario13_DualRoleUserInDistributorContext_Rejected() throws Exception {
        String email = "dual@gmail.com";
        User dualUser = new User(120L, "Dual Role User", email, hashedPassword);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(dualUser));
        when(userRoleRepository.findByUser(dualUser)).thenReturn(List.of(
                new UserRole(8L, dualUser, customerRole),
                new UserRole(9L, dualUser, distributorRole)
        ));

        String unscopedToken = jwtTokenProvider.generateToken(120L, email);

        RoleSelectionRequestDTO selectRequest = new RoleSelectionRequestDTO("DISTRIBUTOR");

        mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + unscopedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selectRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // =========================================================================
    // SCENARIO 14: Case-insensitivity check (Manager@Approved-Distributor.Example) -> 200 OK
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 14: Email with mixed-case approved domain is normalized and succeeds (200 OK)")
    void testScenario14_CaseInsensitiveDomainMatching_Success() throws Exception {
        String email = "Manager@Approved-Distributor.Example";
        User distUser = new User(140L, "Mixed Case Manager", email, hashedPassword);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(distUser));
        when(userRoleRepository.findByUser(distUser)).thenReturn(List.of(
                new UserRole(10L, distUser, distributorRole)
        ));

        DistributorMembership membership = new DistributorMembership(2L, distUser, distributorOrg, "MANAGER", "ACTIVE");
        when(distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(140L, "ACTIVE"))
                .thenReturn(List.of(membership));

        String loginPayload = "{\"email\":\"" + email + "\",\"password\":\"" + rawPassword + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("DISTRIBUTOR"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // =========================================================================
    // SCENARIO 15: Subdomain / suffix spoofing check -> 400 Bad Request
    // =========================================================================
    @Test
    @DisplayName("SCENARIO 15: Exact domain matching prevents subdomain/suffix spoofing attacks (400 Bad Request)")
    void testScenario15_SubdomainSuffixSpoofing_Rejected() throws Exception {
        String spoofedEmail = "user@approved-distributor.example.attacker.com";
        User spoofer = new User(150L, "Spoofed Suffix", spoofedEmail, hashedPassword);
        when(userRepository.findByEmail(spoofedEmail)).thenReturn(Optional.of(spoofer));
        when(userRoleRepository.findByUser(spoofer)).thenReturn(List.of(
                new UserRole(11L, spoofer, distributorRole)
        ));

        String unscopedToken = jwtTokenProvider.generateToken(150L, spoofedEmail);

        RoleSelectionRequestDTO selectRequest = new RoleSelectionRequestDTO("DISTRIBUTOR");

        mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + unscopedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selectRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
