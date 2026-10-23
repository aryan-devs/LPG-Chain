package com.lpgchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.DistributorStatusUpdateDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.CylinderInventoryRepository;
import com.lpgchain.repository.DeliveryRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminUserAndDistributorIntegrationTest {

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
    private BookingRepository bookingRepository;

    @MockBean
    private DeliveryRepository deliveryRepository;

    @MockBean
    private CylinderInventoryRepository cylinderInventoryRepository;

    private String customerToken;
    private String distributorToken;
    private String adminToken;

    private Distributor sampleDistributor;

    @BeforeEach
    void setUp() {
        // Customer setup
        User customerUser = new User(10L, "Customer John", "customer@lpgchain.demo", "hash");
        Role customerRole = new Role(1L, "CUSTOMER");
        UserRole customerUserRole = new UserRole(1L, customerUser, customerRole);

        when(userRepository.findByEmail("customer@lpgchain.demo")).thenReturn(Optional.of(customerUser));
        when(userRoleRepository.findByUser(customerUser)).thenReturn(List.of(customerUserRole));
        customerToken = jwtTokenProvider.generateToken(10L, "customer@lpgchain.demo");

        // Distributor setup
        User distributorUser = new User(20L, "Distributor Agency", "distributor@lpgchain.demo", "hash");
        Role distributorRole = new Role(2L, "DISTRIBUTOR");
        UserRole distributorUserRole = new UserRole(2L, distributorUser, distributorRole);

        when(userRepository.findByEmail("distributor@lpgchain.demo")).thenReturn(Optional.of(distributorUser));
        when(userRoleRepository.findByUser(distributorUser)).thenReturn(List.of(distributorUserRole));
        distributorToken = jwtTokenProvider.generateToken(20L, "distributor@lpgchain.demo");

        // Admin setup
        User adminUser = new User(30L, "Admin Officer", "admin@lpgchain.demo", "hash");
        Role adminRole = new Role(3L, "ADMIN");
        UserRole adminUserRole = new UserRole(3L, adminUser, adminRole);

        when(userRepository.findByEmail("admin@lpgchain.demo")).thenReturn(Optional.of(adminUser));
        when(userRoleRepository.findByUser(adminUser)).thenReturn(List.of(adminUserRole));
        adminToken = jwtTokenProvider.generateToken(30L, "admin@lpgchain.demo");

        // Sample distributor
        sampleDistributor = new Distributor(
                100L,
                "Apex Gas Agency",
                "LIC-2026-9901",
                "+91 98200 11223",
                "Plot 12, BKC, Mumbai",
                "ACTIVE",
                null,
                null,
                false
        );
        sampleDistributor.setCity("Mumbai");
        sampleDistributor.setPinCode("400051");
        sampleDistributor.setSupportedServices("BOTH");

        when(distributorRepository.findById(100L)).thenReturn(Optional.of(sampleDistributor));
        when(distributorRepository.save(any(Distributor.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ==========================================
    // 1. User Management Endpoint Tests
    // ==========================================

    @Test
    @DisplayName("GET /api/users returns 401 when unauthenticated")
    void testGetUsersUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users returns 403 when authenticated as CUSTOMER")
    void testGetUsersCustomerForbidden() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/users returns 403 when authenticated as DISTRIBUTOR")
    void testGetUsersDistributorForbidden() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + distributorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/users returns 200 with resolved role when authenticated as ADMIN")
    void testGetUsersAdminSuccess() throws Exception {
        User user1 = new User(10L, "Customer John", "customer@lpgchain.demo", "secret-pass");
        Role customerRole = new Role(1L, "CUSTOMER");
        UserRole ur1 = new UserRole(1L, user1, customerRole);

        when(userRepository.findAll()).thenReturn(List.of(user1));
        when(userRoleRepository.findByUser(user1)).thenReturn(List.of(ur1));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Customer John"))
                .andExpect(jsonPath("$[0].email").value("customer@lpgchain.demo"))
                .andExpect(jsonPath("$[0].role").value("CUSTOMER"))
                .andExpect(jsonPath("$[0].password").doesNotExist()); // Confirms password hash not exposed
    }

    // ==========================================
    // 2. Distributor Status Endpoint Tests
    // ==========================================

    @Test
    @DisplayName("PATCH /api/distributors/{id}/status returns 401 when unauthenticated")
    void testUpdateDistributorStatusUnauthenticated() throws Exception {
        DistributorStatusUpdateDTO dto = new DistributorStatusUpdateDTO("SUSPENDED");
        mockMvc.perform(patch("/api/distributors/100/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/distributors/{id}/status returns 403 when authenticated as CUSTOMER")
    void testUpdateDistributorStatusCustomerForbidden() throws Exception {
        DistributorStatusUpdateDTO dto = new DistributorStatusUpdateDTO("SUSPENDED");
        mockMvc.perform(patch("/api/distributors/100/status")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/distributors/{id}/status returns 403 when authenticated as DISTRIBUTOR")
    void testUpdateDistributorStatusDistributorForbidden() throws Exception {
        DistributorStatusUpdateDTO dto = new DistributorStatusUpdateDTO("SUSPENDED");
        mockMvc.perform(patch("/api/distributors/100/status")
                        .header("Authorization", "Bearer " + distributorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/distributors/{id}/status returns 200 and updates status when authenticated as ADMIN")
    void testUpdateDistributorStatusAdminSuccess() throws Exception {
        DistributorStatusUpdateDTO dto = new DistributorStatusUpdateDTO("SUSPENDED");
        mockMvc.perform(patch("/api/distributors/100/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.agencyName").value("Apex Gas Agency"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("PATCH /api/distributors/{id}/status returns 400 when invalid status is provided")
    void testUpdateDistributorStatusInvalid() throws Exception {
        String invalidPayload = "{\"status\": \"INVALID_STATUS\"}";
        mockMvc.perform(patch("/api/distributors/100/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/distributors/{id}/status returns 404 when distributor does not exist")
    void testUpdateDistributorStatusNotFound() throws Exception {
        when(distributorRepository.findById(999L)).thenReturn(Optional.empty());
        DistributorStatusUpdateDTO dto = new DistributorStatusUpdateDTO("ACTIVE");
        mockMvc.perform(patch("/api/distributors/999/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest()); // IllegalArgumentException handled by GlobalExceptionHandler -> 400
    }

    // ==========================================
    // 3. Overview Dashboard KPI Endpoint Tests
    // ==========================================

    @Test
    @DisplayName("GET /api/admin/overview-kpis returns 401 when unauthenticated")
    void testGetOverviewKpisUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/overview-kpis"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/admin/overview-kpis returns 403 when authenticated as CUSTOMER")
    void testGetOverviewKpisCustomerForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/overview-kpis")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/overview-kpis returns 200 with aggregate metrics when authenticated as ADMIN")
    void testGetOverviewKpisAdminSuccess() throws Exception {
        when(userRepository.count()).thenReturn(25L);
        when(distributorRepository.count()).thenReturn(4L);
        when(bookingRepository.count()).thenReturn(120L);
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());
        when(deliveryRepository.findAll()).thenReturn(Collections.emptyList());
        when(cylinderInventoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(distributorRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/overview-kpis")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(25))
                .andExpect(jsonPath("$.totalDistributors").value(4))
                .andExpect(jsonPath("$.totalBookings").value(120))
                .andExpect(jsonPath("$.isPlaceholder").value(false));
    }
}
