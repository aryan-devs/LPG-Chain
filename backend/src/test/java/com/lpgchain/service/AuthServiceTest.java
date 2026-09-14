package com.lpgchain.service;

import com.lpgchain.config.EmailPolicyProperties;
import com.lpgchain.dto.DistributorSelectionRequestDTO;
import com.lpgchain.dto.DistributorSelectionResponseDTO;
import com.lpgchain.dto.LoginRequestDTO;
import com.lpgchain.dto.LoginResponseDTO;
import com.lpgchain.dto.RoleSelectionRequestDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.exception.InvalidCredentialsException;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.security.JwtTokenProvider;
import com.lpgchain.security.PasswordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private DistributorMembershipRepository distributorMembershipRepository;

    @Mock
    private DistributorRepository distributorRepository;

    private PasswordService passwordService;
    private JwtTokenProvider jwtTokenProvider;
    private EmailPolicyService emailPolicyService;
    private AuthService authService;

    private User singleRoleCustomer;
    private User multiRoleUser;
    private Role customerRole;
    private Role distributorRole;
    private Role adminRole;

    private final String rawPassword = "TestPassword@123";
    private String passwordHash;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService(new BCryptPasswordEncoder(), "test-pepper-12345");
        jwtTokenProvider = new JwtTokenProvider("TestJwtSecretKeyForAutomatedSecurityTestingMustBeAtLeast256BitsLongForHmacSha256!", 3600000);
        emailPolicyService = new EmailPolicyService(new EmailPolicyProperties(
                "distributor.example,agency.example,approved-distributor.example,lpgchain.demo",
                "lpgchain.example,admin.lpgchain.example,lpgchain.demo"
        ));

        authService = new AuthService(
                userRepository,
                userRoleRepository,
                distributorMembershipRepository,
                distributorRepository,
                passwordService,
                jwtTokenProvider,
                emailPolicyService
        );

        customerRole = new Role(1L, "CUSTOMER");
        distributorRole = new Role(2L, "DISTRIBUTOR");
        adminRole = new Role(3L, "ADMIN");

        passwordHash = passwordService.encode(rawPassword);

        singleRoleCustomer = new User(10L, "Single Role User", "customer@example.com", passwordHash);
        multiRoleUser = new User(20L, "Multi Role User", "multi@approved-distributor.example", passwordHash);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // TEST 1: Valid email + correct password -> authentication succeeds
    // =========================================================================
    @Test
    @DisplayName("TEST 1: Valid email and correct password authenticates successfully")
    void shouldLoginWithValidCredentials() {
        LoginRequestDTO request = new LoginRequestDTO("customer@example.com", rawPassword);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(singleRoleCustomer));
        when(userRoleRepository.findByUser(singleRoleCustomer)).thenReturn(List.of(
                new UserRole(1L, singleRoleCustomer, customerRole)
        ));

        LoginResponseDTO response = authService.login(request);

        assertNotNull(response);
        assertEquals("customer@example.com", response.getEmail());
        assertNotNull(response.getToken());
        assertTrue(jwtTokenProvider.validateToken(response.getToken()));
        assertEquals("CUSTOMER", jwtTokenProvider.getRoleFromToken(response.getToken()));
        assertEquals("Login successful", response.getMessage());
    }

    // =========================================================================
    // TEST 2: Valid email + wrong password -> authentication fails
    // =========================================================================
    @Test
    @DisplayName("TEST 2: Valid email with incorrect password throws InvalidCredentialsException")
    void shouldRejectInvalidPassword() {
        LoginRequestDTO request = new LoginRequestDTO("customer@example.com", "WrongPassword!999");

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(singleRoleCustomer));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    // =========================================================================
    // TEST 3: Email does not exist -> authentication fails
    // =========================================================================
    @Test
    @DisplayName("TEST 3: Non-existent email throws InvalidCredentialsException without exposing user presence")
    void shouldRejectUnknownUser() {
        LoginRequestDTO request = new LoginRequestDTO("nonexistent@example.com", rawPassword);

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    // =========================================================================
    // TEST 4: Password verification uses the existing BCrypt + pepper flow
    // =========================================================================
    @Test
    @DisplayName("TEST 4: Password verification uses existing BCrypt + pepper flow correctly")
    void shouldVerifyPasswordUsingPasswordServicePepperFlow() {
        LoginRequestDTO request = new LoginRequestDTO("customer@example.com", rawPassword);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(singleRoleCustomer));
        when(userRoleRepository.findByUser(singleRoleCustomer)).thenReturn(List.of(
                new UserRole(1L, singleRoleCustomer, customerRole)
        ));

        LoginResponseDTO response = authService.login(request);
        assertNotNull(response);

        // Verify that raw password verified against peppered hash directly passes via PasswordService
        assertTrue(passwordService.matches(rawPassword, passwordHash));
        assertFalse(passwordService.matches("WrongPassword", passwordHash));
    }

    // =========================================================================
    // TEST 5: User with one role -> single-role behavior
    // =========================================================================
    @Test
    @DisplayName("TEST 5: Single-role user login generates immediate active-role JWT with roleSelectionRequired=false")
    void shouldProduceSingleRoleBehaviorForSingleRoleUser() {
        LoginRequestDTO request = new LoginRequestDTO("customer@example.com", rawPassword);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(singleRoleCustomer));
        when(userRoleRepository.findByUser(singleRoleCustomer)).thenReturn(List.of(
                new UserRole(1L, singleRoleCustomer, customerRole)
        ));

        LoginResponseDTO response = authService.login(request);

        assertFalse(response.isRoleSelectionRequired());
        assertEquals("CUSTOMER", response.getActiveRole());
        assertEquals(List.of("CUSTOMER"), response.getRoles());
        assertNotNull(response.getToken());
        assertEquals("CUSTOMER", jwtTokenProvider.getRoleFromToken(response.getToken()));
    }

    // =========================================================================
    // TEST 6: User with multiple roles -> role selection is required
    // =========================================================================
    @Test
    @DisplayName("TEST 6: Multi-role user login generates unscoped JWT with roleSelectionRequired=true")
    void shouldRequireRoleSelectionForMultipleRoles() {
        LoginRequestDTO request = new LoginRequestDTO("multi@approved-distributor.example", rawPassword);

        when(userRepository.findByEmail("multi@approved-distributor.example")).thenReturn(Optional.of(multiRoleUser));
        when(userRoleRepository.findByUser(multiRoleUser)).thenReturn(List.of(
                new UserRole(2L, multiRoleUser, customerRole),
                new UserRole(3L, multiRoleUser, distributorRole)
        ));

        LoginResponseDTO response = authService.login(request);

        assertTrue(response.isRoleSelectionRequired());
        assertNull(response.getActiveRole());
        assertEquals(List.of("CUSTOMER", "DISTRIBUTOR"), response.getRoles());
        assertNotNull(response.getToken());
        // Unscoped token does not have active role
        assertNull(jwtTokenProvider.getRoleFromToken(response.getToken()));
        assertEquals("Multiple roles available. Select a role.", response.getMessage());
    }

    // =========================================================================
    // TEST 7: Selected role belongs to user -> role selection succeeds
    // =========================================================================
    @Test
    @DisplayName("TEST 7: User selecting an assigned role succeeds and receives active-role JWT")
    void shouldSucceedWhenSelectedRoleBelongsToUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("multi@approved-distributor.example", null, List.of())
        );

        when(userRepository.findByEmail("multi@approved-distributor.example")).thenReturn(Optional.of(multiRoleUser));
        when(userRoleRepository.findByUser(multiRoleUser)).thenReturn(List.of(
                new UserRole(2L, multiRoleUser, customerRole),
                new UserRole(3L, multiRoleUser, distributorRole)
        ));

        RoleSelectionRequestDTO request = new RoleSelectionRequestDTO("CUSTOMER");
        LoginResponseDTO response = authService.selectRole(request);

        assertNotNull(response);
        assertEquals("CUSTOMER", response.getActiveRole());
        assertFalse(response.isRoleSelectionRequired());
        assertNotNull(response.getToken());
        assertEquals("CUSTOMER", jwtTokenProvider.getRoleFromToken(response.getToken()));
    }

    // =========================================================================
    // TEST 8: Selected role does NOT belong to user -> privilege escalation blocked
    // =========================================================================
    @Test
    @DisplayName("TEST 8: User attempting to select unassigned role (ADMIN) is rejected with AccessDeniedException")
    void shouldRejectUnauthorizedRoleSelection() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("multi@approved-distributor.example", null, List.of())
        );

        when(userRepository.findByEmail("multi@approved-distributor.example")).thenReturn(Optional.of(multiRoleUser));
        when(userRoleRepository.findByUser(multiRoleUser)).thenReturn(List.of(
                new UserRole(2L, multiRoleUser, customerRole),
                new UserRole(3L, multiRoleUser, distributorRole)
        ));

        // User only has CUSTOMER and DISTRIBUTOR; attempts to select ADMIN
        RoleSelectionRequestDTO request = new RoleSelectionRequestDTO("ADMIN");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> authService.selectRole(request));
        assertTrue(ex.getMessage().contains("Role ADMIN is not assigned"));
    }

    // =========================================================================
    // TEST 9: Non-existent user attempting role selection -> rejected
    // =========================================================================
    @Test
    @DisplayName("TEST 9: Non-existent user in SecurityContext is rejected with AccessDeniedException")
    void shouldRejectRoleSelectionForUnknownUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("unknown@example.com", null, List.of())
        );

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RoleSelectionRequestDTO request = new RoleSelectionRequestDTO("CUSTOMER");
        assertThrows(AccessDeniedException.class, () -> authService.selectRole(request));
    }

    // =========================================================================
    // TEST 10: Distributor Selection with valid vs invalid membership
    // =========================================================================
    @Test
    @DisplayName("TEST 10A: Distributor selection with active membership succeeds and generates organization-scoped JWT")
    void shouldSucceedDistributorSelectionWithActiveMembership() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dist@distributor.example", null, List.of())
        );

        User distUser = new User(30L, "Dist User", "dist@distributor.example", passwordHash);
        Distributor distributor = new Distributor();
        distributor.setId(100L);
        distributor.setAgencyName("Apex Gas");

        DistributorMembership membership = new DistributorMembership(1L, distUser, distributor, "MANAGER", "ACTIVE");

        when(userRepository.findByEmail("dist@distributor.example")).thenReturn(Optional.of(distUser));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(30L, 100L))
                .thenReturn(Optional.of(membership));

        DistributorSelectionRequestDTO request = new DistributorSelectionRequestDTO(100L);
        DistributorSelectionResponseDTO response = authService.selectDistributor(request);

        assertNotNull(response);
        assertEquals(100L, response.getDistributorId());
        assertEquals("Apex Gas", response.getAgencyName());
        assertNotNull(response.getToken());
        assertEquals("DISTRIBUTOR", jwtTokenProvider.getRoleFromToken(response.getToken()));
        assertEquals(100L, jwtTokenProvider.getDistributorIdFromToken(response.getToken()));
    }

    @Test
    @DisplayName("TEST 10B: Distributor selection without active membership is rejected with AccessDeniedException")
    void shouldRejectDistributorSelectionWithoutActiveMembership() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dist@distributor.example", null, List.of())
        );

        User distUser = new User(30L, "Dist User", "dist@distributor.example", passwordHash);

        when(userRepository.findByEmail("dist@distributor.example")).thenReturn(Optional.of(distUser));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(30L, 200L))
                .thenReturn(Optional.empty());

        DistributorSelectionRequestDTO request = new DistributorSelectionRequestDTO(200L);
        assertThrows(AccessDeniedException.class, () -> authService.selectDistributor(request));
    }
}
