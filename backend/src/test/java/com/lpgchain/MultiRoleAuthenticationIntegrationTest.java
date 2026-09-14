package com.lpgchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.RoleSelectionRequestDTO;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MultiRoleAuthenticationIntegrationTest {

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

    private User singleRoleUser;
    private User multiRoleUser;
    private Role customerRole;
    private Role distributorRole;
    private Role adminRole;

    private String rawPassword = "SecurePassword@123";
    private String hashedPassword;

    @BeforeEach
    void setUp() {
        hashedPassword = passwordService.encode(rawPassword);

        customerRole = new Role(1L, "CUSTOMER");
        distributorRole = new Role(2L, "DISTRIBUTOR");
        adminRole = new Role(3L, "ADMIN");

        // 1. Single-role user: CUSTOMER only
        singleRoleUser = new User(10L, "Single Role User", "single@lpgchain.demo", hashedPassword);
        when(userRepository.findByEmail("single@lpgchain.demo")).thenReturn(Optional.of(singleRoleUser));
        when(userRoleRepository.findByUser(singleRoleUser)).thenReturn(List.of(
                new UserRole(1L, singleRoleUser, customerRole)
        ));

        // 2. Multi-role user: CUSTOMER + DISTRIBUTOR
        multiRoleUser = new User(20L, "Multi Role User", "multi@lpgchain.demo", hashedPassword);
        when(userRepository.findByEmail("multi@lpgchain.demo")).thenReturn(Optional.of(multiRoleUser));
        when(userRoleRepository.findByUser(multiRoleUser)).thenReturn(List.of(
                new UserRole(2L, multiRoleUser, customerRole),
                new UserRole(3L, multiRoleUser, distributorRole)
        ));
    }

    // =========================================================================
    // TEST A — Single-Role Login
    // =========================================================================

    @Test
    @DisplayName("TEST A: Single-role user receives active-role JWT immediately (roleSelectionRequired=false)")
    void testSingleRoleUserLogin() throws Exception {
        String loginPayload = "{\"email\":\"single@lpgchain.demo\",\"password\":\"" + rawPassword + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.email").value("single@lpgchain.demo"))
                .andExpect(jsonPath("$.roles").value(containsInAnyOrder("CUSTOMER")))
                .andExpect(jsonPath("$.roleSelectionRequired").value(false))
                .andExpect(jsonPath("$.activeRole").value("CUSTOMER"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    // =========================================================================
    // TEST B — Multiple Roles Login
    // =========================================================================

    @Test
    @DisplayName("TEST B: Multi-role user login returns available roles and requires role selection")
    void testMultiRoleUserLogin() throws Exception {
        String loginPayload = "{\"email\":\"multi@lpgchain.demo\",\"password\":\"" + rawPassword + "\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(20))
                .andExpect(jsonPath("$.email").value("multi@lpgchain.demo"))
                .andExpect(jsonPath("$.roles").value(containsInAnyOrder("CUSTOMER", "DISTRIBUTOR")))
                .andExpect(jsonPath("$.roleSelectionRequired").value(true))
                .andExpect(jsonPath("$.activeRole").value(nullValue()))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.message").value("Multiple roles available. Select a role."));
    }

    // =========================================================================
    // TEST C — Select CUSTOMER Role
    // =========================================================================

    @Test
    @DisplayName("TEST C: Multi-role user selects CUSTOMER -> gets active CUSTOMER token (customer allowed, distributor denied)")
    void testSelectCustomerRole() throws Exception {
        // Multi-role token without active role
        String initialToken = jwtTokenProvider.generateToken(20L, "multi@lpgchain.demo");

        RoleSelectionRequestDTO requestDTO = new RoleSelectionRequestDTO("CUSTOMER");

        MvcResult result = mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(20))
                .andExpect(jsonPath("$.email").value("multi@lpgchain.demo"))
                .andExpect(jsonPath("$.activeRole").value("CUSTOMER"))
                .andExpect(jsonPath("$.roleSelectionRequired").value(false))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.message").value("Role selected successfully"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String activeToken = objectMapper.readTree(responseBody).get("token").asText();
        assertNotNull(activeToken);

        // Verify permissions with active CUSTOMER token
        mockMvc.perform(get("/api/customer/test")
                        .header("Authorization", "Bearer " + activeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        mockMvc.perform(get("/api/distributor/test")
                        .header("Authorization", "Bearer " + activeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // TEST D — Select DISTRIBUTOR Role
    // =========================================================================

    @Test
    @DisplayName("TEST D: Multi-role user selects DISTRIBUTOR -> gets active DISTRIBUTOR token (distributor allowed, customer denied)")
    void testSelectDistributorRole() throws Exception {
        String initialToken = jwtTokenProvider.generateToken(20L, "multi@lpgchain.demo");

        RoleSelectionRequestDTO requestDTO = new RoleSelectionRequestDTO("DISTRIBUTOR");

        MvcResult result = mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("DISTRIBUTOR"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String activeToken = objectMapper.readTree(responseBody).get("token").asText();
        assertNotNull(activeToken);

        // Verify permissions with active DISTRIBUTOR token
        mockMvc.perform(get("/api/distributor/test")
                        .header("Authorization", "Bearer " + activeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        mockMvc.perform(get("/api/customer/test")
                        .header("Authorization", "Bearer " + activeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // TEST E — Privilege Escalation Attempt
    // =========================================================================

    @Test
    @DisplayName("TEST E: Privilege escalation attempt -> 403 Forbidden (requesting ADMIN when only CUSTOMER & DISTRIBUTOR assigned)")
    void testPrivilegeEscalationAttempt() throws Exception {
        String initialToken = jwtTokenProvider.generateToken(20L, "multi@lpgchain.demo");

        RoleSelectionRequestDTO requestDTO = new RoleSelectionRequestDTO("ADMIN");

        mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Role ADMIN is not assigned to user"));
    }

    // =========================================================================
    // TEST F — No JWT on select-role
    // =========================================================================

    @Test
    @DisplayName("TEST F: POST /api/auth/select-role without authentication returns 401 Unauthorized")
    void testSelectRoleWithoutJwtReturns401() throws Exception {
        RoleSelectionRequestDTO requestDTO = new RoleSelectionRequestDTO("CUSTOMER");

        mockMvc.perform(post("/api/auth/select-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    // =========================================================================
    // TEST G — Invalid JWT on select-role
    // =========================================================================

    @Test
    @DisplayName("TEST G: POST /api/auth/select-role with invalid JWT returns 401 Unauthorized")
    void testSelectRoleWithInvalidJwtReturns401() throws Exception {
        RoleSelectionRequestDTO requestDTO = new RoleSelectionRequestDTO("CUSTOMER");

        mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer invalid.token.value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // =========================================================================
    // TEST H — Role Normalization on selection
    // =========================================================================

    @Test
    @DisplayName("TEST H: Role normalization accepts lowercase or ROLE_ prefix and normalizes cleanly")
    void testRoleNormalizationOnSelection() throws Exception {
        String initialToken = jwtTokenProvider.generateToken(20L, "multi@lpgchain.demo");

        // Client passes lowercase "customer"
        mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"customer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("CUSTOMER"));

        // Client passes "ROLE_CUSTOMER"
        mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("CUSTOMER"));
    }
}
