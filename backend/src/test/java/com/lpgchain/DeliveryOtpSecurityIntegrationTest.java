package com.lpgchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.DeliveryOtpVerificationDTO;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeliveryOtpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private String customerToken;
    private String distributorAToken;
    private String distributorBToken;
    private String driverToken;
    private String adminToken;

    private User customer;
    private User distributorUserA;
    private User distributorUserB;
    private User driverUser;
    private User adminUser;

    private Distributor distributorOrgA;
    private Distributor distributorOrgB;

    private Driver driverA;
    private Vehicle vehicleA;
    private Booking bookingA;
    private Delivery deliveryA;

    @BeforeEach
    void setUp() {
        Role customerRole = new Role(1L, "CUSTOMER");
        Role distributorRole = new Role(2L, "DISTRIBUTOR");
        Role driverRole = new Role(3L, "DRIVER");
        Role adminRole = new Role(4L, "ADMIN");

        customer = new User(101L, "Customer Rahul", "customer@lpgchain.demo", "hash", "+919876543210", "Flat 101");
        distributorUserA = new User(102L, "Distributor Staff A", "staffA@distributor.example", "hash");
        distributorUserB = new User(103L, "Distributor Staff B", "staffB@agency.example", "hash");
        driverUser = new User(104L, "Driver Ramesh", "driver@distributor.example", "hash", "+919123456789", "Street 5");
        adminUser = new User(105L, "Admin Boss", "admin@lpgchain.example", "hash");

        distributorOrgA = new Distributor();
        distributorOrgA.setId(1L);
        distributorOrgA.setAgencyName("Apex Gas Agency");
        distributorOrgA.setLicenceNumber("DIS-001");

        distributorOrgB = new Distributor();
        distributorOrgB.setId(2L);
        distributorOrgB.setAgencyName("Beta Gas Agency");
        distributorOrgB.setLicenceNumber("DIS-002");

        driverA = new Driver(10L, "Driver Ramesh", "+919123456789", "LIC-777", "ON_DELIVERY", distributorOrgA);
        vehicleA = new Vehicle(5L, "MH-12-QQ-1111", "LPG_TRUCK", 30, "IN_TRANSIT", distributorOrgA);

        bookingA = new Booking(1001L, customer, distributorOrgA, "Flat 101", LocalDateTime.now(), 1, "OUT_FOR_DELIVERY");

        deliveryA = new Delivery(501L, bookingA, driverA, vehicleA, distributorOrgA, "OUT_FOR_DELIVERY",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().minusMinutes(10), null);

        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        when(userRepository.findByEmail(distributorUserA.getEmail())).thenReturn(Optional.of(distributorUserA));
        when(userRepository.findByEmail(distributorUserB.getEmail())).thenReturn(Optional.of(distributorUserB));
        when(userRepository.findByEmail(driverUser.getEmail())).thenReturn(Optional.of(driverUser));
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

        when(userRoleRepository.findByUser(customer)).thenReturn(List.of(new UserRole(customer, customerRole)));
        when(userRoleRepository.findByUser(distributorUserA)).thenReturn(List.of(new UserRole(distributorUserA, distributorRole)));
        when(userRoleRepository.findByUser(distributorUserB)).thenReturn(List.of(new UserRole(distributorUserB, distributorRole)));
        when(userRoleRepository.findByUser(driverUser)).thenReturn(List.of(new UserRole(driverUser, driverRole)));
        when(userRoleRepository.findByUser(adminUser)).thenReturn(List.of(new UserRole(adminUser, adminRole)));

        DistributorMembership membershipA = new DistributorMembership(distributorUserA, distributorOrgA, "STAFF", "ACTIVE");
        DistributorMembership membershipB = new DistributorMembership(distributorUserB, distributorOrgB, "STAFF", "ACTIVE");
        DistributorMembership driverMembership = new DistributorMembership(driverUser, distributorOrgA, "DRIVER", "ACTIVE");

        when(distributorMembershipRepository.findByUserId(distributorUserA.getId())).thenReturn(List.of(membershipA));
        when(distributorMembershipRepository.findByUserId(distributorUserB.getId())).thenReturn(List.of(membershipB));
        when(distributorMembershipRepository.findByUserId(driverUser.getId())).thenReturn(List.of(driverMembership));

        when(distributorMembershipRepository.findByUserIdAndDistributorId(distributorUserA.getId(), 1L)).thenReturn(Optional.of(membershipA));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(distributorUserB.getId(), 2L)).thenReturn(Optional.of(membershipB));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(driverUser.getId(), 1L)).thenReturn(Optional.of(driverMembership));

        customerToken = "Bearer " + jwtTokenProvider.generateToken(customer.getId(), customer.getEmail(), "CUSTOMER");
        distributorAToken = "Bearer " + jwtTokenProvider.generateToken(distributorUserA.getId(), distributorUserA.getEmail(), "DISTRIBUTOR", 1L);
        distributorBToken = "Bearer " + jwtTokenProvider.generateToken(distributorUserB.getId(), distributorUserB.getEmail(), "DISTRIBUTOR", 2L);
        driverToken = "Bearer " + jwtTokenProvider.generateToken(driverUser.getId(), driverUser.getEmail(), "DRIVER");
        adminToken = "Bearer " + jwtTokenProvider.generateToken(adminUser.getId(), adminUser.getEmail(), "ADMIN");
    }

    @Test
    @DisplayName("1. Unauthenticated request to initiate-handover returns 401")
    void unauthenticated_InitiateHandover_Returns401() throws Exception {
        mockMvc.perform(post("/api/deliveries/501/initiate-handover"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("2. Customer cannot initiate handover OTP (returns 403 Forbidden)")
    void customer_CannotInitiateHandover() throws Exception {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));

        mockMvc.perform(post("/api/deliveries/501/initiate-handover")
                        .header("Authorization", customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. Customer cannot submit/verify delivery handover OTP (returns 403 Forbidden)")
    void customer_CannotVerifyOtp() throws Exception {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));

        DeliveryOtpVerificationDTO dto = new DeliveryOtpVerificationDTO("123456");

        mockMvc.perform(post("/api/deliveries/501/verify-otp")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. Mismatched Distributor B cannot initiate handover for Distributor A's delivery (returns 403)")
    void distributorB_CannotInitiateHandover() throws Exception {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));

        mockMvc.perform(post("/api/deliveries/501/initiate-handover")
                        .header("Authorization", distributorBToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("5. Assigned Distributor A can initiate handover (returns 200 OK)")
    void distributorA_CanInitiateHandover() throws Exception {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/deliveries/501/initiate-handover")
                        .header("Authorization", distributorAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryId").value(501))
                .andExpect(jsonPath("$.status").value("OUT_FOR_DELIVERY"))
                .andExpect(jsonPath("$.handoverStage").value("OTP_DISPATCHED"));
    }

    @Test
    @DisplayName("6. Driver can verify valid OTP and transition delivery to DELIVERED")
    void driver_CanVerifyOtpAndCompleteDelivery() throws Exception {
        deliveryA.setOtpHash(passwordEncoder.encode("456789"));
        deliveryA.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        deliveryA.setOtpAttempts(0);
        deliveryA.setOtpMaxAttempts(3);

        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        DeliveryOtpVerificationDTO dto = new DeliveryOtpVerificationDTO("456789");

        mockMvc.perform(post("/api/deliveries/501/verify-otp")
                        .header("Authorization", driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(501))
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.deliveredAt").isNotEmpty());
    }

    @Test
    @DisplayName("7. Delivery GET response never exposes the actual OTP or OTP hash")
    void deliveryGet_NeverExposesOtp() throws Exception {
        deliveryA.setOtpHash(passwordEncoder.encode("456789"));
        deliveryA.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        deliveryA.setOtpGeneratedAt(LocalDateTime.now());

        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));

        mockMvc.perform(get("/api/deliveries/501")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(501))
                .andExpect(jsonPath("$.handoverInitiated").value(true))
                .andExpect(jsonPath("$.otp").doesNotExist())
                .andExpect(jsonPath("$.otpHash").doesNotExist())
                .andExpect(jsonPath("$.devTestOtp").doesNotExist());
    }
}
