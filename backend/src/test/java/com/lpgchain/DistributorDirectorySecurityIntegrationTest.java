package com.lpgchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DistributorDirectorySecurityIntegrationTest
 *
 * Verifies end-to-end security, sanitization, search filtering, and role enforcement
 * for the distributor directory module.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DistributorDirectorySecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DistributorRepository distributorRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserRoleRepository userRoleRepository;

    private Distributor distMumbaiDomestic;
    private Distributor distMumbaiCommercial;
    private Distributor distDelhiBoth;
    private Distributor distNoCoords;

    private User adminUser;
    private User customerUser;
    private Role adminRole;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role(1L, "ADMIN");
        customerRole = new Role(2L, "CUSTOMER");

        adminUser = new User(10L, "Admin User", "admin@lpgchain.demo", "hashedPass");
        customerUser = new User(20L, "Customer User", "customer@lpgchain.demo", "hashedPass");

        when(userRepository.findByEmail("admin@lpgchain.demo")).thenReturn(Optional.of(adminUser));
        when(userRepository.findByEmail("customer@lpgchain.demo")).thenReturn(Optional.of(customerUser));
        when(userRoleRepository.findByUser(adminUser)).thenReturn(List.of(new UserRole(adminUser, adminRole)));
        when(userRoleRepository.findByUser(customerUser)).thenReturn(List.of(new UserRole(customerUser, customerRole)));

        distMumbaiDomestic = new Distributor(
                1L, "Mumbai Domestic Agency", "LIC-MUM-01", "+919876500001",
                "10 Marine Drive", "ACTIVE", "topSecretPasskey1",
                LocalDateTime.now().plusDays(3), false,
                "400001", "Mumbai", "Maharashtra",
                18.9220, 72.8347, "DOMESTIC", "Mon-Sat 09:00-18:00"
        );

        distMumbaiCommercial = new Distributor(
                2L, "Mumbai Bulk Fuels", "LIC-MUM-02", "+919876500002",
                "50 Industrial Park", "ACTIVE", "topSecretPasskey2",
                LocalDateTime.now().plusDays(3), false,
                "400050", "Mumbai", "Maharashtra",
                19.0550, 72.8300, "COMMERCIAL", "Mon-Sun 08:00-20:00"
        );

        distDelhiBoth = new Distributor(
                3L, "Delhi Prime Gas", "LIC-DEL-01", "+919876500003",
                "100 Connaught Place", "ACTIVE", "topSecretPasskey3",
                LocalDateTime.now().plusDays(3), false,
                "110001", "New Delhi", "Delhi",
                28.6315, 77.2167, "BOTH", "24x7 Emergency"
        );

        // Distributor with missing coordinates
        distNoCoords = new Distributor(
                4L, "Rural Express Gas", "LIC-RUR-01", "+919876500004",
                "Village Post Office Road", "ACTIVE", "topSecretPasskey4",
                LocalDateTime.now().plusDays(3), false,
                "415001", "Satara", "Maharashtra",
                null, null, "DOMESTIC", "Mon-Fri 10:00-17:00"
        );
    }

    // =========================================================================
    // 1. PUBLIC DIRECTORY SEARCH (ANONYMOUS ACCESS & SANITIZATION)
    // =========================================================================

    @Test
    @DisplayName("SEC-DIR-01: Anonymous public search succeeds and strictly excludes sensitive passkeys and credentials")
    void searchDistributors_anonymousAccessAllowed_andExcludesCredentials() throws Exception {
        when(distributorRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(distMumbaiDomestic, distDelhiBoth));

        mockMvc.perform(get("/api/distributors/public/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].agencyName").value("Mumbai Domestic Agency"))
                .andExpect(jsonPath("$[0].contactNumber").value("+919876500001"))
                .andExpect(jsonPath("$[0].address").value("10 Marine Drive"))
                .andExpect(jsonPath("$[0].pinCode").value("400001"))
                .andExpect(jsonPath("$[0].city").value("Mumbai"))
                .andExpect(jsonPath("$[0].state").value("Maharashtra"))
                .andExpect(jsonPath("$[0].latitude").value(18.9220))
                .andExpect(jsonPath("$[0].longitude").value(72.8347))
                .andExpect(jsonPath("$[0].supportedServices").value("DOMESTIC"))
                .andExpect(jsonPath("$[0].operatingHours").value("Mon-Sat 09:00-18:00"))
                .andExpect(jsonPath("$[0].networkType").value("LPG_CHAIN_PARTNER"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                // STRICT SECURITY: internal passkey credentials must NEVER appear in output
                .andExpect(jsonPath("$[0].passkey").doesNotExist())
                .andExpect(jsonPath("$[0].passkeyExpiry").doesNotExist())
                .andExpect(jsonPath("$[0].passkeyUsed").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[1].passkey").doesNotExist());
    }

    @Test
    @DisplayName("SEC-DIR-02: Public search with PIN code, city, and service type filters")
    void searchDistributors_withValidFilters_returnsMatchingDistributors() throws Exception {
        when(distributorRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(distMumbaiDomestic));

        mockMvc.perform(get("/api/distributors/public/search")
                        .param("pinCode", "400001")
                        .param("city", "mumbai")
                        .param("serviceType", "DOMESTIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].agencyName").value("Mumbai Domestic Agency"))
                .andExpect(jsonPath("$[0].pinCode").value("400001"));
    }

    @Test
    @DisplayName("SEC-DIR-03: Public search rejects invalid service type with structured 400 Bad Request")
    void searchDistributors_invalidServiceType_returns400() throws Exception {
        mockMvc.perform(get("/api/distributors/public/search")
                        .param("serviceType", "AGRICULTURAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Invalid service type: 'AGRICULTURAL'")))
                .andExpect(jsonPath("$.message", containsString("Supported values are DOMESTIC, COMMERCIAL, or BOTH")));
    }

    @Test
    @DisplayName("SEC-DIR-04: Public search returns empty array 200 OK when no distributors match")
    void searchDistributors_noMatches_returnsEmptyArray() throws Exception {
        when(distributorRepository.findAll(any(Specification.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/distributors/public/search")
                        .param("city", "Atlantis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("SEC-DIR-05: Missing location coordinates preserved as null without fabricating positions or distances")
    void searchDistributors_handlesMissingCoordinatesWithoutInventingData() throws Exception {
        when(distributorRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(distNoCoords));

        mockMvc.perform(get("/api/distributors/public/search")
                        .param("city", "Satara"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].agencyName").value("Rural Express Gas"))
                .andExpect(jsonPath("$[0].latitude").doesNotExist())
                .andExpect(jsonPath("$[0].longitude").doesNotExist());
    }

    // =========================================================================
    // 2. PROTECTED ADMINISTRATIVE & GET ENDPOINTS
    // =========================================================================

    @Test
    @DisplayName("SEC-DIR-06: General GET /api/distributors requires authentication (401 Unauthorized for anonymous)")
    void getAllDistributors_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/distributors"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("SEC-DIR-07: GET /api/distributors/{id} requires authentication (401 Unauthorized for anonymous)")
    void getDistributorById_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/distributors/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("SEC-DIR-08: Authenticated customer can retrieve sanitized distributor list without credential exposure")
    void getAllDistributors_authenticatedCustomer_returnsSanitizedDTOs() throws Exception {
        String token = jwtTokenProvider.generateToken(20L, "customer@lpgchain.demo", "CUSTOMER", null);
        when(distributorRepository.findAll()).thenReturn(List.of(distMumbaiDomestic, distDelhiBoth));

        mockMvc.perform(get("/api/distributors")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].passkey").doesNotExist())
                .andExpect(jsonPath("$[0].passkeyExpiry").doesNotExist())
                .andExpect(jsonPath("$[1].passkey").doesNotExist());
    }

    @Test
    @DisplayName("SEC-DIR-09: Authenticated customer can retrieve distributor by ID sanitized")
    void getDistributorById_authenticatedCustomer_returnsSanitizedDTO() throws Exception {
        String token = jwtTokenProvider.generateToken(20L, "customer@lpgchain.demo", "CUSTOMER", null);
        when(distributorRepository.findById(1L)).thenReturn(Optional.of(distMumbaiDomestic));

        mockMvc.perform(get("/api/distributors/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.agencyName").value("Mumbai Domestic Agency"))
                .andExpect(jsonPath("$.passkey").doesNotExist())
                .andExpect(jsonPath("$.passkeyExpiry").doesNotExist());
    }

    @Test
    @DisplayName("SEC-DIR-10: POST /api/distributors is protected: 401 for anonymous, 403 for CUSTOMER, 201 for ADMIN")
    void createDistributor_roleEnforcement() throws Exception {
        Distributor newAgency = new Distributor(
                null, "Green Gas", "LIC-GRN-01", "+919876500099",
                "New Green Road", "ACTIVE", "newPasskey",
                LocalDateTime.now().plusDays(5), false,
                "560001", "Bengaluru", "Karnataka",
                12.9716, 77.5946, "BOTH", "09:00 - 18:00"
        );
        Distributor savedAgency = new Distributor(
                100L, "Green Gas", "LIC-GRN-01", "+919876500099",
                "New Green Road", "ACTIVE", "newPasskey",
                LocalDateTime.now().plusDays(5), false,
                "560001", "Bengaluru", "Karnataka",
                12.9716, 77.5946, "BOTH", "09:00 - 18:00"
        );
        when(distributorRepository.save(any(Distributor.class))).thenReturn(savedAgency);

        // 1. Anonymous -> 401
        mockMvc.perform(post("/api/distributors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newAgency)))
                .andExpect(status().isUnauthorized());

        // 2. Customer -> 403 Forbidden
        String customerToken = jwtTokenProvider.generateToken(20L, "customer@lpgchain.demo", "CUSTOMER", null);
        mockMvc.perform(post("/api/distributors")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newAgency)))
                .andExpect(status().isForbidden());

        // 3. Admin -> 201 Created and returned as sanitized DTO
        String adminToken = jwtTokenProvider.generateToken(10L, "admin@lpgchain.demo", "ADMIN", null);
        mockMvc.perform(post("/api/distributors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newAgency)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.agencyName").value("Green Gas"))
                .andExpect(jsonPath("$.city").value("Bengaluru"))
                .andExpect(jsonPath("$.passkey").doesNotExist());
    }

    @Test
    @DisplayName("SEC-DIR-11: DELETE /api/distributors/{id} is protected: 401 for anonymous, 403 for CUSTOMER, 204 for ADMIN")
    void deleteDistributor_roleEnforcement() throws Exception {
        // 1. Anonymous -> 401
        mockMvc.perform(delete("/api/distributors/1"))
                .andExpect(status().isUnauthorized());

        // 2. Customer -> 403
        String customerToken = jwtTokenProvider.generateToken(20L, "customer@lpgchain.demo", "CUSTOMER", null);
        mockMvc.perform(delete("/api/distributors/1")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // 3. Admin -> 204 No Content
        String adminToken = jwtTokenProvider.generateToken(10L, "admin@lpgchain.demo", "ADMIN", null);
        mockMvc.perform(delete("/api/distributors/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        verify(distributorRepository).deleteById(1L);
    }
}
