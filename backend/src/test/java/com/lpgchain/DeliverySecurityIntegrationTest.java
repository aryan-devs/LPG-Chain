package com.lpgchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.DeliveryRequestDTO;
import com.lpgchain.dto.DeliveryStatusUpdateDTO;
import com.lpgchain.dto.DriverRequestDTO;
import com.lpgchain.dto.VehicleRequestDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Driver;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.DriverRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.repository.VehicleRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeliverySecurityIntegrationTest {

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
    private BookingRepository bookingRepository;

    @MockBean
    private DistributorRepository distributorRepository;

    @MockBean
    private DistributorMembershipRepository distributorMembershipRepository;

    @MockBean
    private DriverRepository driverRepository;

    @MockBean
    private VehicleRepository vehicleRepository;

    @MockBean
    private DeliveryRepository deliveryRepository;

    private String customerAToken;
    private String customerBToken;
    private String distributorAToken;
    private String distributorBToken;
    private String adminToken;

    private User customerA;
    private User customerB;
    private User distributorUserA;
    private User distributorUserB;
    private User adminUser;

    private Distributor distributorOrgA;
    private Distributor distributorOrgB;

    private Driver driverA;
    private Driver driverB;

    private Vehicle vehicleA;
    private Vehicle vehicleB;

    private Booking bookingA;
    private Booking bookingB;

    private Delivery deliveryA;

    @BeforeEach
    void setUp() {
        // Roles
        Role customerRole = new Role(1L, "CUSTOMER");
        Role distributorRole = new Role(2L, "DISTRIBUTOR");
        Role adminRole = new Role(3L, "ADMIN");

        // Users
        customerA = new User(101L, "Customer A", "customerA@lpgchain.demo", "hash");
        customerB = new User(102L, "Customer B", "customerB@lpgchain.demo", "hash");
        distributorUserA = new User(201L, "Distributor User A", "distributorA@lpgchain.demo", "hash");
        distributorUserB = new User(202L, "Distributor User B", "distributorB@lpgchain.demo", "hash");
        adminUser = new User(301L, "Admin User", "admin@lpgchain.demo", "hash");

        // Distributor Orgs
        distributorOrgA = new Distributor(1L, "City Gas Agency A", "LIC-A1", "+919000000001", "Address A", "ACTIVE", "passkeyA", LocalDateTime.now().plusDays(1), false);
        distributorOrgB = new Distributor(2L, "Metro Gas Agency B", "LIC-B2", "+919000000002", "Address B", "ACTIVE", "passkeyB", LocalDateTime.now().plusDays(1), false);

        // Drivers & Vehicles
        driverA = new Driver(10L, "Driver A", "+919100000001", "LIC-DRV-A", "AVAILABLE", distributorOrgA);
        driverB = new Driver(20L, "Driver B", "+919100000002", "LIC-DRV-B", "AVAILABLE", distributorOrgB);

        vehicleA = new Vehicle(5L, "MH-01-A-1111", "LPG_DELIVERY", 30, "AVAILABLE", distributorOrgA);
        vehicleB = new Vehicle(6L, "MH-02-B-2222", "LPG_DELIVERY", 25, "AVAILABLE", distributorOrgB);

        // Bookings
        bookingA = new Booking(1001L, customerA, distributorOrgA, "Address A", LocalDateTime.now(), 2, "CONFIRMED");
        bookingB = new Booking(1002L, customerB, distributorOrgB, "Address B", LocalDateTime.now(), 1, "CONFIRMED");

        // Deliveries
        deliveryA = new Delivery(501L, bookingA, driverA, vehicleA, distributorOrgA, "ASSIGNED",
                LocalDateTime.now(), null, null, null);

        // User Lookups
        when(userRepository.findByEmail(customerA.getEmail())).thenReturn(Optional.of(customerA));
        when(userRepository.findByEmail(customerB.getEmail())).thenReturn(Optional.of(customerB));
        when(userRepository.findByEmail(distributorUserA.getEmail())).thenReturn(Optional.of(distributorUserA));
        when(userRepository.findByEmail(distributorUserB.getEmail())).thenReturn(Optional.of(distributorUserB));
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

        // User Roles
        when(userRoleRepository.findByUser(customerA)).thenReturn(List.of(new UserRole(customerA, customerRole)));
        when(userRoleRepository.findByUser(customerB)).thenReturn(List.of(new UserRole(customerB, customerRole)));
        when(userRoleRepository.findByUser(distributorUserA)).thenReturn(List.of(new UserRole(distributorUserA, distributorRole)));
        when(userRoleRepository.findByUser(distributorUserB)).thenReturn(List.of(new UserRole(distributorUserB, distributorRole)));
        when(userRoleRepository.findByUser(adminUser)).thenReturn(List.of(new UserRole(adminUser, adminRole)));

        // Distributor Memberships
        DistributorMembership membershipA = new DistributorMembership(distributorUserA, distributorOrgA, "STAFF", "ACTIVE");
        DistributorMembership membershipB = new DistributorMembership(distributorUserB, distributorOrgB, "STAFF", "ACTIVE");

        when(distributorMembershipRepository.findByUserId(distributorUserA.getId())).thenReturn(List.of(membershipA));
        when(distributorMembershipRepository.findByUserId(distributorUserB.getId())).thenReturn(List.of(membershipB));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(distributorUserA.getId(), distributorOrgA.getId()))
                .thenReturn(Optional.of(membershipA));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(distributorUserB.getId(), distributorOrgB.getId()))
                .thenReturn(Optional.of(membershipB));

        // Tokens
        customerAToken = "Bearer " + jwtTokenProvider.generateToken(customerA.getId(), customerA.getEmail(), "CUSTOMER");
        customerBToken = "Bearer " + jwtTokenProvider.generateToken(customerB.getId(), customerB.getEmail(), "CUSTOMER");
        distributorAToken = "Bearer " + jwtTokenProvider.generateToken(distributorUserA.getId(), distributorUserA.getEmail(), "DISTRIBUTOR", 1L);
        distributorBToken = "Bearer " + jwtTokenProvider.generateToken(distributorUserB.getId(), distributorUserB.getEmail(), "DISTRIBUTOR", 2L);
        adminToken = "Bearer " + jwtTokenProvider.generateToken(adminUser.getId(), adminUser.getEmail(), "ADMIN");
    }

    @Test
    @DisplayName("1. Unauthenticated request to /api/deliveries/501 returns 401")
    void unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/deliveries/501"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("2. Customer A can access their own delivery")
    void customerA_CanAccessOwnDelivery() throws Exception {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));

        mockMvc.perform(get("/api/deliveries/501")
                        .header("Authorization", customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(501))
                .andExpect(jsonPath("$.customerId").value(101))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    @DisplayName("3. Customer B cannot access Customer A's delivery (returns 403)")
    void customerB_CannotAccessCustomerADelivery() throws Exception {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));

        mockMvc.perform(get("/api/deliveries/501")
                        .header("Authorization", customerBToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Customer cannot create a driver (returns 403)")
    void customer_CannotCreateDriver() throws Exception {
        DriverRequestDTO dto = new DriverRequestDTO("New Driver", "+919999999999", "LIC-X", 1L, "AVAILABLE");

        mockMvc.perform(post("/api/drivers")
                        .header("Authorization", customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Customer cannot create a vehicle (returns 403)")
    void customer_CannotCreateVehicle() throws Exception {
        VehicleRequestDTO dto = new VehicleRequestDTO("MH-99-ZZ-9999", "TRUCK", 50, 1L, "AVAILABLE");

        mockMvc.perform(post("/api/vehicles")
                        .header("Authorization", customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Customer cannot modify delivery status (returns 403)")
    void customer_CannotModifyDeliveryStatus() throws Exception {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));

        DeliveryStatusUpdateDTO dto = new DeliveryStatusUpdateDTO("PICKED_UP");

        mockMvc.perform(patch("/api/deliveries/501/status")
                        .header("Authorization", customerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. Distributor A can access their own distributor deliveries")
    void distributorA_CanAccessOwnDeliveries() throws Exception {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));

        mockMvc.perform(get("/api/deliveries/501")
                        .header("Authorization", distributorAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(501))
                .andExpect(jsonPath("$.distributorId").value(1));
    }

    @Test
    @DisplayName("8. Distributor B cannot access Distributor A's deliveries (returns 403)")
    void distributorB_CannotAccessDistributorADeliveries() throws Exception {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));

        mockMvc.perform(get("/api/deliveries/501")
                        .header("Authorization", distributorBToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("9. Distributor A cannot assign another distributor's driver (returns 400)")
    void distributorA_CannotAssignDistributorBDriver() throws Exception {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));
        when(driverRepository.findById(20L)).thenReturn(Optional.of(driverB)); // Driver B is from distributorOrgB

        mockMvc.perform(patch("/api/deliveries/501/assign-driver/20")
                        .header("Authorization", distributorAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("does not belong to delivery distributor")));
    }

    @Test
    @DisplayName("10. Distributor A cannot assign another distributor's vehicle (returns 400)")
    void distributorA_CannotAssignDistributorBVehicle() throws Exception {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));
        when(vehicleRepository.findById(6L)).thenReturn(Optional.of(vehicleB)); // Vehicle B is from distributorOrgB

        mockMvc.perform(patch("/api/deliveries/501/assign-vehicle/6")
                        .header("Authorization", distributorAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("does not belong to delivery distributor")));
    }

    @Test
    @DisplayName("11. Admin can manage delivery resources and view any delivery")
    void admin_CanAccessAnyDelivery() throws Exception {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));

        mockMvc.perform(get("/api/deliveries/501")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(501));
    }

    @Test
    @DisplayName("12. Valid delivery lifecycle transitions: ASSIGNED -> PICKED_UP -> OUT_FOR_DELIVERY -> DELIVERED")
    void deliveryLifecycle_Success() throws Exception {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. ASSIGNED -> PICKED_UP
        DeliveryStatusUpdateDTO step1 = new DeliveryStatusUpdateDTO("PICKED_UP");
        mockMvc.perform(patch("/api/deliveries/501/status")
                        .header("Authorization", distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(step1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"));

        // 2. PICKED_UP -> OUT_FOR_DELIVERY
        DeliveryStatusUpdateDTO step2 = new DeliveryStatusUpdateDTO("OUT_FOR_DELIVERY");
        mockMvc.perform(patch("/api/deliveries/501/status")
                        .header("Authorization", distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(step2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUT_FOR_DELIVERY"));

        // 3. OUT_FOR_DELIVERY -> DELIVERED
        DeliveryStatusUpdateDTO step3 = new DeliveryStatusUpdateDTO("DELIVERED");
        mockMvc.perform(patch("/api/deliveries/501/status")
                        .header("Authorization", distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(step3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    @DisplayName("13. Invalid status transition is rejected with 400")
    void invalidStatusTransition_RejectedWith400() throws Exception {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));

        // Attempt invalid skip ASSIGNED -> DELIVERED
        DeliveryStatusUpdateDTO invalidStep = new DeliveryStatusUpdateDTO("DELIVERED");
        mockMvc.perform(patch("/api/deliveries/501/status")
                        .header("Authorization", distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidStep)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not allowed")));
    }

    @Test
    @DisplayName("14. Cancelled booking cannot create delivery (returns 400)")
    void cancelledBooking_CannotCreateDelivery() throws Exception {
        bookingA.setStatus("CANCELLED");
        when(bookingRepository.findById(1001L)).thenReturn(Optional.of(bookingA));

        DeliveryRequestDTO dto = new DeliveryRequestDTO(1001L, 10L, 5L, 1L);

        mockMvc.perform(post("/api/deliveries")
                        .header("Authorization", distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("CANCELLED")));
    }
}
