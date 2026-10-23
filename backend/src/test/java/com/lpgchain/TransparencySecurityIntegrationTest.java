package com.lpgchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.AllocationAdjustmentRequestDTO;
import com.lpgchain.dto.CylinderAllocationRequestDTO;
import com.lpgchain.entity.CylinderAllocation;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.repository.AllocationAdjustmentRepository;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.CylinderAllocationRepository;
import com.lpgchain.repository.DeliveryRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransparencySecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DistributorRepository distributorRepository;

    @MockBean
    private CylinderAllocationRepository allocationRepository;

    @MockBean
    private AllocationAdjustmentRepository adjustmentRepository;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private DeliveryRepository deliveryRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserRoleRepository userRoleRepository;

    @MockBean
    private DistributorMembershipRepository distributorMembershipRepository;

    private Distributor dist1;
    private Distributor dist2;
    private CylinderAllocation alloc1;

    private User adminUser;
    private User customerUser;
    private User distUser1;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role(1L, "ADMIN");
        Role customerRole = new Role(2L, "CUSTOMER");
        Role distRole = new Role(3L, "DISTRIBUTOR");

        adminUser = new User(10L, "Admin User", "admin@lpgchain.test", "hashedPass");
        customerUser = new User(20L, "Customer User", "customer@lpgchain.test", "hashedPass");
        distUser1 = new User(30L, "Distributor 1 User", "dist1@lpgchain.test", "hashedPass");

        when(userRepository.findByEmail("admin@lpgchain.test")).thenReturn(Optional.of(adminUser));
        when(userRepository.findByEmail("customer@lpgchain.test")).thenReturn(Optional.of(customerUser));
        when(userRepository.findByEmail("dist1@lpgchain.test")).thenReturn(Optional.of(distUser1));

        when(userRoleRepository.findByUser(adminUser)).thenReturn(List.of(new UserRole(adminUser, adminRole)));
        when(userRoleRepository.findByUser(customerUser)).thenReturn(List.of(new UserRole(customerUser, customerRole)));
        when(userRoleRepository.findByUser(distUser1)).thenReturn(List.of(new UserRole(distUser1, distRole)));

        dist1 = new Distributor(
                1L, "Apex Gas Agency", "LIC-001", "+919876543210",
                "10 MG Road", "ACTIVE", "secretPasskey1",
                LocalDateTime.now().plusDays(2), false,
                "400001", "Mumbai", "Maharashtra",
                18.9220, 72.8347, "DOMESTIC", "09:00 - 18:00"
        );

        dist2 = new Distributor(
                2L, "Bharat Fuels", "LIC-002", "+919876543211",
                "25 Linking Road", "ACTIVE", "secretPasskey2",
                LocalDateTime.now().plusDays(2), false,
                "400050", "Mumbai", "Maharashtra",
                19.0550, 72.8300, "COMMERCIAL", "09:00 - 18:00"
        );

        when(distributorRepository.findById(1L)).thenReturn(Optional.of(dist1));
        when(distributorRepository.findById(2L)).thenReturn(Optional.of(dist2));

        alloc1 = new CylinderAllocation(
                dist1,
                "DOMESTIC_14_2",
                14.2,
                1000,
                2026,
                10,
                LocalDate.of(2026, 10, 1),
                "OMC-OCT-2026",
                "admin@lpgchain.test"
        );
        alloc1.setId(101L);

        when(allocationRepository.findById(101L)).thenReturn(Optional.of(alloc1));
        when(allocationRepository.findByDistributorIdAndReportingYearAndReportingMonth(1L, 2026, 10))
                .thenReturn(List.of(alloc1));
        when(allocationRepository.findByDistributorId(1L))
                .thenReturn(List.of(alloc1));
        when(allocationRepository.findDistinctReportingPeriods())
                .thenReturn(List.of("2026-10", "2026-09"));

        when(bookingRepository.findByDistributorIdAndStatus(1L, "DELIVERED"))
                .thenReturn(Collections.emptyList());
        when(deliveryRepository.findByDistributorId(1L))
                .thenReturn(Collections.emptyList());
        when(adjustmentRepository.findByAllocationIdOrderByAdjustedAtDesc(101L))
                .thenReturn(Collections.emptyList());

        // Membership for distUser1 in dist1
        DistributorMembership m1 = new DistributorMembership(distUser1, dist1, "OWNER", "ACTIVE");
        when(distributorMembershipRepository.findByUserIdAndDistributorId(30L, 1L))
                .thenReturn(Optional.of(m1));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(30L, 2L))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("SEC-TRANS-01: Anonymous public can access transparency report, no credentials exposed")
    void getTransparencyReport_AnonymousPublic_SuccessAndSanitized() throws Exception {
        mockMvc.perform(get("/api/transparency/distributors/1")
                        .param("year", "2026")
                        .param("month", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distributorId").value(1))
                .andExpect(jsonPath("$.distributorName").value("Apex Gas Agency"))
                .andExpect(jsonPath("$.reportingPeriod").value("2026-10"))
                .andExpect(jsonPath("$.categories", hasSize(3)))
                // Security verification: NO credentials or internal keys exposed
                .andExpect(jsonPath("$.passkey").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.distributorPasskey").doesNotExist());
    }

    @Test
    @DisplayName("SEC-TRANS-02: Public can access available reporting periods and categories")
    void getPeriodsAndCategories_AnonymousPublic_Success() throws Exception {
        mockMvc.perform(get("/api/transparency/periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/transparency/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("SEC-TRANS-03: Customer role can access transparency report read-only")
    void getTransparencyReport_CustomerRole_Success() throws Exception {
        String customerToken = jwtTokenProvider.generateToken(20L, "customer@lpgchain.test", "CUSTOMER", null);

        mockMvc.perform(get("/api/transparency/distributors/1")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distributorId").value(1));
    }

    @Test
    @DisplayName("SEC-TRANS-04: Customer role cannot create cylinder allocation (403 Forbidden)")
    void createAllocation_CustomerRole_Forbidden() throws Exception {
        String customerToken = jwtTokenProvider.generateToken(20L, "customer@lpgchain.test", "CUSTOMER", null);

        CylinderAllocationRequestDTO request = new CylinderAllocationRequestDTO();
        request.setDistributorId(1L);
        request.setCylinderCategory("DOMESTIC_14_2");
        request.setAllocatedQuantity(500);
        request.setReportingYear(2026);
        request.setReportingMonth(10);

        mockMvc.perform(post("/api/allocations")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-TRANS-05: Customer role cannot add quota adjustment (403 Forbidden)")
    void addAdjustment_CustomerRole_Forbidden() throws Exception {
        String customerToken = jwtTokenProvider.generateToken(20L, "customer@lpgchain.test", "CUSTOMER", null);

        AllocationAdjustmentRequestDTO request = new AllocationAdjustmentRequestDTO();
        request.setAdjustmentType("CORRECTION");
        request.setAdjustedQuantity(10);
        request.setReason("Unauthorized test");

        mockMvc.perform(post("/api/allocations/101/adjustments")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-TRANS-06: Admin role can successfully create allocation")
    void createAllocation_AdminRole_Success() throws Exception {
        String adminToken = jwtTokenProvider.generateToken(10L, "admin@lpgchain.test", "ADMIN", null);

        CylinderAllocationRequestDTO request = new CylinderAllocationRequestDTO();
        request.setDistributorId(1L);
        request.setCylinderCategory("DOMESTIC_5_0");
        request.setAllocatedQuantity(200);
        request.setReportingYear(2026);
        request.setReportingMonth(10);

        when(allocationRepository.findByDistributorIdAndCylinderCategoryAndReportingYearAndReportingMonth(
                1L, "DOMESTIC_5_0", 2026, 10)).thenReturn(Optional.empty());
        when(allocationRepository.save(any(CylinderAllocation.class))).thenAnswer(inv -> {
            CylinderAllocation a = inv.getArgument(0);
            a.setId(102L);
            return a;
        });

        mockMvc.perform(post("/api/allocations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(102))
                .andExpect(jsonPath("$.allocatedQuantity").value(200));
    }

    @Test
    @DisplayName("SEC-TRANS-07: Cross-distributor access rejection (Distributor 1 cannot view Distributor 2 allocations)")
    void getDistributorAllocations_CrossDistributor_Forbidden() throws Exception {
        // Distributor 1 user attempts to view Distributor 2's allocations
        String dist1Token = jwtTokenProvider.generateToken(30L, "dist1@lpgchain.test", "DISTRIBUTOR", 1L);

        mockMvc.perform(get("/api/allocations/distributor/2")
                        .header("Authorization", "Bearer " + dist1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-TRANS-08: Distributor 1 can view its own internal allocations")
    void getDistributorAllocations_OwnDistributor_Success() throws Exception {
        String dist1Token = jwtTokenProvider.generateToken(30L, "dist1@lpgchain.test", "DISTRIBUTOR", 1L);

        mockMvc.perform(get("/api/allocations/distributor/1")
                        .header("Authorization", "Bearer " + dist1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].allocatedQuantity").value(1000));
    }

    @Test
    @DisplayName("SEC-TRANS-09: Unauthenticated mutation to /api/allocations returns 401 Unauthorized")
    void createAllocation_Unauthenticated_Unauthorized() throws Exception {
        CylinderAllocationRequestDTO request = new CylinderAllocationRequestDTO();
        request.setDistributorId(1L);
        request.setCylinderCategory("DOMESTIC_14_2");
        request.setAllocatedQuantity(500);
        request.setReportingYear(2026);
        request.setReportingMonth(10);

        mockMvc.perform(post("/api/allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
