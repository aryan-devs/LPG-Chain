package com.lpgchain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.DistributorSelectionRequestDTO;
import com.lpgchain.dto.LoginRequestDTO;
import com.lpgchain.dto.RoleSelectionRequestDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
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
class AuthControllerSecurityTest {

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
    private DistributorRepository distributorRepository;

    @MockBean
    private DistributorMembershipRepository distributorMembershipRepository;

    private User customerUser;
    private User multiRoleUser;
    private Role customerRole;
    private Role distributorRole;
    private Role adminRole;

    private final String rawPassword = "CorrectPassword@123";
    private String hashedPassword;

    @BeforeEach
    void setUp() {
        hashedPassword = passwordService.encode(rawPassword);

        customerRole = new Role(1L, "CUSTOMER");
        distributorRole = new Role(2L, "DISTRIBUTOR");
        adminRole = new Role(3L, "ADMIN");

        customerUser = new User(10L, "Customer User", "customer@lpgchain.demo", hashedPassword);
        multiRoleUser = new User(20L, "Multi Role User", "multi@approved-distributor.example", hashedPassword);
    }

    // =========================================================================
    // POST /api/auth/login
    // =========================================================================
    @Test
    @DisplayName("AUTH CONTROLLER: POST /api/auth/login with valid credentials returns 200 OK + JWT")
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        when(userRepository.findByEmail("customer@lpgchain.demo")).thenReturn(Optional.of(customerUser));
        when(userRoleRepository.findByUser(customerUser)).thenReturn(List.of(
                new UserRole(1L, customerUser, customerRole)
        ));

        LoginRequestDTO request = new LoginRequestDTO("customer@lpgchain.demo", rawPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer@lpgchain.demo"))
                .andExpect(jsonPath("$.activeRole").value("CUSTOMER"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.roleSelectionRequired").value(false));
    }

    @Test
    @DisplayName("AUTH CONTROLLER: POST /api/auth/login with incorrect password returns 401 Unauthorized")
    void shouldReturn401WhenPasswordIsIncorrect() throws Exception {
        when(userRepository.findByEmail("customer@lpgchain.demo")).thenReturn(Optional.of(customerUser));

        LoginRequestDTO request = new LoginRequestDTO("customer@lpgchain.demo", "WrongPassword!999");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("AUTH CONTROLLER: POST /api/auth/login with blank fields returns 400 Bad Request")
    void shouldReturn400WhenCredentialsAreBlank() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    // =========================================================================
    // POST /api/auth/select-role
    // =========================================================================
    @Test
    @DisplayName("AUTH CONTROLLER: POST /api/auth/select-role without token returns 401 Unauthorized")
    void shouldReturn401WhenSelectingRoleWithoutToken() throws Exception {
        RoleSelectionRequestDTO request = new RoleSelectionRequestDTO("CUSTOMER");

        mockMvc.perform(post("/api/auth/select-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("AUTH CONTROLLER: POST /api/auth/select-role with valid token and assigned role returns 200 OK")
    void shouldReturn200WhenSelectingAssignedRole() throws Exception {
        when(userRepository.findByEmail("multi@approved-distributor.example")).thenReturn(Optional.of(multiRoleUser));
        when(userRoleRepository.findByUser(multiRoleUser)).thenReturn(List.of(
                new UserRole(2L, multiRoleUser, customerRole),
                new UserRole(3L, multiRoleUser, distributorRole)
        ));

        String unscopedToken = jwtTokenProvider.generateToken(20L, "multi@approved-distributor.example");

        RoleSelectionRequestDTO request = new RoleSelectionRequestDTO("DISTRIBUTOR");

        mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + unscopedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeRole").value("DISTRIBUTOR"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("AUTH CONTROLLER: POST /api/auth/select-role with unassigned role (ADMIN) returns 403 Forbidden")
    void shouldReturn403WhenSelectingUnassignedRole() throws Exception {
        when(userRepository.findByEmail("multi@approved-distributor.example")).thenReturn(Optional.of(multiRoleUser));
        when(userRoleRepository.findByUser(multiRoleUser)).thenReturn(List.of(
                new UserRole(2L, multiRoleUser, customerRole),
                new UserRole(3L, multiRoleUser, distributorRole)
        ));

        String unscopedToken = jwtTokenProvider.generateToken(20L, "multi@approved-distributor.example");

        RoleSelectionRequestDTO request = new RoleSelectionRequestDTO("ADMIN");

        mockMvc.perform(post("/api/auth/select-role")
                        .header("Authorization", "Bearer " + unscopedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // POST /api/auth/select-distributor
    // =========================================================================
    @Test
    @DisplayName("AUTH CONTROLLER: POST /api/auth/select-distributor without token returns 401 Unauthorized")
    void shouldReturn401WhenSelectingDistributorWithoutToken() throws Exception {
        DistributorSelectionRequestDTO request = new DistributorSelectionRequestDTO(100L);

        mockMvc.perform(post("/api/auth/select-distributor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("AUTH CONTROLLER: POST /api/auth/select-distributor without active membership returns 403 Forbidden")
    void shouldReturn403WhenSelectingDistributorWithoutMembership() throws Exception {
        when(userRepository.findByEmail("multi@approved-distributor.example")).thenReturn(Optional.of(multiRoleUser));
        Distributor dist = new Distributor();
        dist.setId(999L);
        when(distributorRepository.findById(999L)).thenReturn(Optional.of(dist));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(20L, 999L))
                .thenReturn(Optional.empty());

        String unscopedToken = jwtTokenProvider.generateToken(20L, "multi@approved-distributor.example");

        DistributorSelectionRequestDTO request = new DistributorSelectionRequestDTO(999L);

        mockMvc.perform(post("/api/auth/select-distributor")
                        .header("Authorization", "Bearer " + unscopedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
