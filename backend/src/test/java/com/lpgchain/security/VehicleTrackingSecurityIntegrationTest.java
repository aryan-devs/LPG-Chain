package com.lpgchain.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.VehicleLocationRequestDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.entity.VehicleLocation;
import com.lpgchain.integration.traccar.TraccarClient;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.repository.VehicleLocationRepository;
import com.lpgchain.repository.VehicleRepository;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VehicleTrackingSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserRoleRepository userRoleRepository;

    @MockBean
    private DistributorRepository distributorRepository;

    @MockBean
    private DistributorMembershipRepository distributorMembershipRepository;

    @MockBean
    private VehicleRepository vehicleRepository;

    @MockBean
    private VehicleLocationRepository vehicleLocationRepository;

    @MockBean
    private DeliveryRepository deliveryRepository;

    @MockBean
    private TraccarClient traccarClient;

    private User adminUser;
    private User customerUserA;
    private User customerUserB;
    private User distributorUserOrg1;
    private User distributorUserOrg2;

    private Distributor distributorOrg1;
    private Distributor distributorOrg2;

    private Vehicle vehicle1;
    private Vehicle vehicle2;

    private Delivery deliveryA;
    private Delivery deliveryB;

    private VehicleLocation location1;

    private String adminToken;
    private String customerAToken;
    private String customerBToken;
    private String distributor1Token;
    private String distributor2Token;

    @BeforeEach
    void setUp() {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("vehicle:101:latest-location");
                redisTemplate.delete("vehicle:202:latest-location");
                redisTemplate.delete("vehicle:10:latest-location");
                redisTemplate.delete("lpgchain:tracking:latest:101");
                redisTemplate.delete("lpgchain:tracking:latest:202");
            } catch (Exception ignored) {
            }
        }
        Role adminRole = new Role(1L, "ADMIN");
        Role customerRole = new Role(2L, "CUSTOMER");
        Role distributorRole = new Role(3L, "DISTRIBUTOR");

        // Admin User
        adminUser = new User(100L, "Admin User", "admin@lpgchain.example", "hash");
        when(userRepository.findByEmail("admin@lpgchain.example")).thenReturn(Optional.of(adminUser));
        when(userRoleRepository.findByUser(adminUser)).thenReturn(List.of(new UserRole(1L, adminUser, adminRole)));
        adminToken = jwtTokenProvider.generateToken(100L, "admin@lpgchain.example", "ADMIN");

        // Customer User A
        customerUserA = new User(1L, "Customer A", "customera@gmail.com", "hash");
        when(userRepository.findByEmail("customera@gmail.com")).thenReturn(Optional.of(customerUserA));
        when(userRoleRepository.findByUser(customerUserA)).thenReturn(List.of(new UserRole(2L, customerUserA, customerRole)));
        customerAToken = jwtTokenProvider.generateToken(1L, "customera@gmail.com", "CUSTOMER");

        // Customer User B
        customerUserB = new User(2L, "Customer B", "customerb@gmail.com", "hash");
        when(userRepository.findByEmail("customerb@gmail.com")).thenReturn(Optional.of(customerUserB));
        when(userRoleRepository.findByUser(customerUserB)).thenReturn(List.of(new UserRole(3L, customerUserB, customerRole)));
        customerBToken = jwtTokenProvider.generateToken(2L, "customerb@gmail.com", "CUSTOMER");

        // Distributor Org 1 & User
        distributorOrg1 = new Distributor();
        distributorOrg1.setId(10L);
        distributorOrg1.setAgencyName("Distributor Org 1");

        distributorUserOrg1 = new User(10L, "Dist 1 User", "dist1@approved-distributor.example", "hash");
        when(userRepository.findByEmail("dist1@approved-distributor.example")).thenReturn(Optional.of(distributorUserOrg1));
        when(userRoleRepository.findByUser(distributorUserOrg1)).thenReturn(List.of(new UserRole(4L, distributorUserOrg1, distributorRole)));
        distributor1Token = jwtTokenProvider.generateToken(10L, "dist1@approved-distributor.example", "DISTRIBUTOR");

        DistributorMembership membership1 = new DistributorMembership(distributorUserOrg1, distributorOrg1, "MANAGER", "ACTIVE");
        membership1.setId(1L);
        when(distributorMembershipRepository.findByUserId(10L)).thenReturn(List.of(membership1));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(10L, 10L)).thenReturn(Optional.of(membership1));

        // Distributor Org 2 & User
        distributorOrg2 = new Distributor();
        distributorOrg2.setId(20L);
        distributorOrg2.setAgencyName("Distributor Org 2");

        distributorUserOrg2 = new User(20L, "Dist 2 User", "dist2@approved-distributor.example", "hash");
        when(userRepository.findByEmail("dist2@approved-distributor.example")).thenReturn(Optional.of(distributorUserOrg2));
        when(userRoleRepository.findByUser(distributorUserOrg2)).thenReturn(List.of(new UserRole(5L, distributorUserOrg2, distributorRole)));
        distributor2Token = jwtTokenProvider.generateToken(20L, "dist2@approved-distributor.example", "DISTRIBUTOR");

        DistributorMembership membership2 = new DistributorMembership(distributorUserOrg2, distributorOrg2, "MANAGER", "ACTIVE");
        membership2.setId(2L);
        when(distributorMembershipRepository.findByUserId(20L)).thenReturn(List.of(membership2));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(20L, 20L)).thenReturn(Optional.of(membership2));

        // Vehicles
        vehicle1 = new Vehicle("MH-01-AB-1234", "LPG_DELIVERY", 30, "AVAILABLE", distributorOrg1);
        vehicle1.setId(101L);
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(vehicle1));

        vehicle2 = new Vehicle("MH-02-CD-5678", "LPG_DELIVERY", 30, "AVAILABLE", distributorOrg2);
        vehicle2.setId(202L);
        when(vehicleRepository.findById(202L)).thenReturn(Optional.of(vehicle2));

        // Bookings & Deliveries
        Booking bookingA = new Booking();
        bookingA.setId(1001L);
        bookingA.setCustomer(customerUserA);

        deliveryA = new Delivery(bookingA, null, vehicle1, distributorOrg1, "ASSIGNED");
        deliveryA.setId(501L);
        when(deliveryRepository.findById(501L)).thenReturn(Optional.of(deliveryA));

        Booking bookingB = new Booking();
        bookingB.setId(1002L);
        bookingB.setCustomer(customerUserB);

        deliveryB = new Delivery(bookingB, null, vehicle2, distributorOrg2, "ASSIGNED");
        deliveryB.setId(502L);
        when(deliveryRepository.findById(502L)).thenReturn(Optional.of(deliveryB));

        // Location
        location1 = new VehicleLocation(vehicle1, 18.6298, 73.7997, 32.5, 180.0, LocalDateTime.of(2026, 9, 17, 18, 30, 0));
        location1.setId(901L);
        when(vehicleLocationRepository.findFirstByVehicleIdOrderByRecordedAtDesc(101L)).thenReturn(Optional.of(location1));
        when(vehicleLocationRepository.save(any(VehicleLocation.class))).thenReturn(location1);
    }

    @Test
    @DisplayName("1. Unauthenticated request -> 401 Unauthorized")
    void unauthenticatedRequest_Returns401() throws Exception {
        mockMvc.perform(get("/api/tracking/vehicles/101/location"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("2. Customer can view own delivery location -> 200 OK")
    void customer_CanViewOwnDeliveryLocation_Returns200() throws Exception {
        mockMvc.perform(get("/api/tracking/deliveries/501/location")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(101L))
                .andExpect(jsonPath("$.vehicleNumber").value("MH-01-AB-1234"))
                .andExpect(jsonPath("$.latitude").value(18.6298));
    }

    @Test
    @DisplayName("3. Customer cannot view another customer's delivery location -> 403 Forbidden")
    void customer_CannotViewAnotherCustomerDeliveryLocation_Returns403() throws Exception {
        mockMvc.perform(get("/api/tracking/deliveries/502/location")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Customer cannot directly access arbitrary vehicle location -> 403 Forbidden")
    void customer_CannotDirectlyAccessVehicleLocation_Returns403() throws Exception {
        mockMvc.perform(get("/api/tracking/vehicles/101/location")
                        .header("Authorization", "Bearer " + customerAToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Distributor can view own distributor vehicle location -> 200 OK")
    void distributor_CanViewOwnVehicleLocation_Returns200() throws Exception {
        mockMvc.perform(get("/api/tracking/vehicles/101/location")
                        .header("Authorization", "Bearer " + distributor1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(101L))
                .andExpect(jsonPath("$.latitude").value(18.6298));
    }

    @Test
    @DisplayName("6. Distributor cannot access another distributor's vehicle location -> 403 Forbidden")
    void distributor_CannotAccessOtherDistributorVehicleLocation_Returns403() throws Exception {
        mockMvc.perform(get("/api/tracking/vehicles/202/location")
                        .header("Authorization", "Bearer " + distributor1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. Distributor cannot access another distributor's delivery tracking -> 403 Forbidden")
    void distributor_CannotAccessOtherDistributorDeliveryTracking_Returns403() throws Exception {
        mockMvc.perform(get("/api/tracking/deliveries/502/location")
                        .header("Authorization", "Bearer " + distributor1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("8. Unauthorized customer cannot submit GPS location -> 403 Forbidden")
    void customer_CannotSubmitVehicleLocation_Returns403() throws Exception {
        VehicleLocationRequestDTO dto = new VehicleLocationRequestDTO(
                18.6298, 73.7997, 25.0, 90.0, LocalDateTime.now()
        );

        mockMvc.perform(post("/api/tracking/vehicles/101/location")
                        .header("Authorization", "Bearer " + customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("9. Admin can access authorized tracking endpoints")
    void admin_CanAccessTrackingEndpoints_Returns200() throws Exception {
        mockMvc.perform(get("/api/tracking/vehicles/101/location")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(101L));

        mockMvc.perform(get("/api/tracking/deliveries/501/location")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(101L));
    }
}
