package com.lpgchain;

import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RbacSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserRoleRepository userRoleRepository;

    private String customerToken;
    private String distributorToken;
    private String adminToken;
    private String adminWithPrefixToken;

    @BeforeEach
    void setUp() {
        // 1. Customer User
        User customerUser = new User(1L, "Customer User", "customer@lpgchain.demo", "hash");
        Role customerRole = new Role(1L, "CUSTOMER");
        UserRole customerUserRole = new UserRole(1L, customerUser, customerRole);

        when(userRepository.findByEmail("customer@lpgchain.demo")).thenReturn(Optional.of(customerUser));
        when(userRoleRepository.findByUser(customerUser)).thenReturn(List.of(customerUserRole));
        customerToken = jwtTokenProvider.generateToken(1L, "customer@lpgchain.demo");

        // 2. Distributor User
        User distributorUser = new User(2L, "Distributor User", "distributor@lpgchain.demo", "hash");
        Role distributorRole = new Role(2L, "DISTRIBUTOR");
        UserRole distributorUserRole = new UserRole(2L, distributorUser, distributorRole);

        when(userRepository.findByEmail("distributor@lpgchain.demo")).thenReturn(Optional.of(distributorUser));
        when(userRoleRepository.findByUser(distributorUser)).thenReturn(List.of(distributorUserRole));
        distributorToken = jwtTokenProvider.generateToken(2L, "distributor@lpgchain.demo");

        // 3. Admin User (Role stored as "ADMIN")
        User adminUser = new User(3L, "Admin User", "admin@lpgchain.demo", "hash");
        Role adminRole = new Role(3L, "ADMIN");
        UserRole adminUserRole = new UserRole(3L, adminUser, adminRole);

        when(userRepository.findByEmail("admin@lpgchain.demo")).thenReturn(Optional.of(adminUser));
        when(userRoleRepository.findByUser(adminUser)).thenReturn(List.of(adminUserRole));
        adminToken = jwtTokenProvider.generateToken(3L, "admin@lpgchain.demo");

        // 4. Admin User with pre-existing "ROLE_ADMIN" prefix in DB to test normalization
        User adminWithPrefixUser = new User(4L, "Admin Prefixed", "admin.prefix@lpgchain.demo", "hash");
        Role adminPrefixedRole = new Role(4L, "ROLE_ADMIN");
        UserRole adminPrefixedUserRole = new UserRole(4L, adminWithPrefixUser, adminPrefixedRole);

        when(userRepository.findByEmail("admin.prefix@lpgchain.demo")).thenReturn(Optional.of(adminWithPrefixUser));
        when(userRoleRepository.findByUser(adminWithPrefixUser)).thenReturn(List.of(adminPrefixedUserRole));
        adminWithPrefixToken = jwtTokenProvider.generateToken(4L, "admin.prefix@lpgchain.demo");
    }

    // ==========================================
    // 1. Unauthenticated & Invalid Token Tests (401)
    // ==========================================

    @Test
    @DisplayName("No JWT should return 401 Unauthorized for protected role endpoints")
    void testNoJwtReturns401() throws Exception {
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
    @DisplayName("Invalid or malformed JWT should return 401 Unauthorized")
    void testInvalidJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/customer/test")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ==========================================
    // 2. CUSTOMER Token Tests (200 for customer, 403 for distributor & admin)
    // ==========================================

    @Test
    @DisplayName("Customer token: /api/customer/test -> 200, /api/distributor/test -> 403, /api/admin/test -> 403")
    void testCustomerTokenAccess() throws Exception {
        // Customer endpoint -> 200 OK
        mockMvc.perform(get("/api/customer/test")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Customer endpoint accessed successfully"));

        // Distributor endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/distributor/test")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Access denied"));

        // Admin endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Access denied"));
    }

    // ==========================================
    // 3. DISTRIBUTOR Token Tests (200 for distributor, 403 for customer & admin)
    // ==========================================

    @Test
    @DisplayName("Distributor token: /api/distributor/test -> 200, /api/customer/test -> 403, /api/admin/test -> 403")
    void testDistributorTokenAccess() throws Exception {
        // Distributor endpoint -> 200 OK
        mockMvc.perform(get("/api/distributor/test")
                        .header("Authorization", "Bearer " + distributorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Distributor endpoint accessed successfully"));

        // Customer endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/customer/test")
                        .header("Authorization", "Bearer " + distributorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Access denied"));

        // Admin endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + distributorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Access denied"));
    }

    // ==========================================
    // 4. ADMIN Token Tests (200 for admin, 403 for customer & distributor)
    // ==========================================

    @Test
    @DisplayName("Admin token: /api/admin/test -> 200, /api/customer/test -> 403, /api/distributor/test -> 403")
    void testAdminTokenAccess() throws Exception {
        // Admin endpoint -> 200 OK
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Admin endpoint accessed successfully"));

        // Customer endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/customer/test")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Access denied"));

        // Distributor endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/distributor/test")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Access denied"));
    }

    // ==========================================
    // 5. Role Normalization (avoid ROLE_ROLE_ADMIN)
    // ==========================================

    @Test
    @DisplayName("Role already prefixed with ROLE_ in database should not produce ROLE_ROLE_ and should match hasRole('ADMIN')")
    void testRoleWithExistingPrefixNormalization() throws Exception {
        mockMvc.perform(get("/api/admin/test")
                        .header("Authorization", "Bearer " + adminWithPrefixToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    // ==========================================
    // 6. Public Endpoints Verification
    // ==========================================

    @Test
    @DisplayName("Public endpoints should not require authentication")
    void testPublicEndpointsRemainPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());

        // POST /api/users should pass through security without 401/403 (fails validation or 400, but not 401)
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(
                        401, result.getResponse().getStatus(), "Public user registration must not return 401"
                ))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(
                        403, result.getResponse().getStatus(), "Public user registration must not return 403"
                ));
    }
}
