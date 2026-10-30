package com.lpgchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.BookingRequestDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.repository.BookingRepository;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
class RealBookingLifecycleIntegrationTest {

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
    private DistributorMembershipRepository distributorMembershipRepository;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private com.lpgchain.repository.PaymentRepository paymentRepository;

    private User customerUser;
    private User distributorUser;
    private Distributor apexAgency;
    private String customerToken;
    private String distributorToken;

    @BeforeEach
    void setUp() {
        // 1. Setup real customer
        customerUser = new User();
        customerUser.setId(101L);
        customerUser.setName("Arman Shak");
        customerUser.setEmail("customer@example.com");
        customerUser.setPhone("+91 98201 11223");
        customerUser.setAddress("Flat 402, Sunshine Heights, Sector 18, Vashi");

        Role customerRole = new Role(1L, "CUSTOMER");
        UserRole customerUserRole = new UserRole(1L, customerUser, customerRole);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(userRepository.findById(101L)).thenReturn(Optional.of(customerUser));
        when(userRoleRepository.findByUser(customerUser)).thenReturn(List.of(customerUserRole));
        customerToken = jwtTokenProvider.generateToken(101L, "customer@example.com");

        // 2. Setup distributor user & agency (Apex Gas Agency, ID 1)
        distributorUser = new User();
        distributorUser.setId(202L);
        distributorUser.setName("Apex Gas Agency Staff");
        distributorUser.setEmail("distributor@agency.example");
        distributorUser.setPhone("+91 98201 54321");

        Role distributorRole = new Role(2L, "DISTRIBUTOR");
        UserRole distributorUserRole = new UserRole(2L, distributorUser, distributorRole);

        apexAgency = new Distributor();
        apexAgency.setId(1L);
        apexAgency.setAgencyName("Apex Gas Agency");
        apexAgency.setLicenceNumber("LPG-MH-PN-1001");
        apexAgency.setStatus("ACTIVE");
        apexAgency.setCity("Navi Mumbai");
        apexAgency.setPinCode("400703");

        DistributorMembership membership = new DistributorMembership(distributorUser, apexAgency, "OWNER", "ACTIVE");
        membership.setId(1L);

        when(userRepository.findByEmail("distributor@agency.example")).thenReturn(Optional.of(distributorUser));
        when(userRepository.findById(202L)).thenReturn(Optional.of(distributorUser));
        when(userRoleRepository.findByUser(distributorUser)).thenReturn(List.of(distributorUserRole));
        when(distributorRepository.findById(1L)).thenReturn(Optional.of(apexAgency));
        when(distributorRepository.findAll()).thenReturn(List.of(apexAgency));
        when(distributorMembershipRepository.findByUserId(202L)).thenReturn(List.of(membership));
        when(distributorMembershipRepository.existsByUserIdAndDistributorId(202L, 1L)).thenReturn(true);
        when(paymentRepository.save(any(com.lpgchain.entity.Payment.class))).thenAnswer(i -> i.getArgument(0));

        distributorToken = jwtTokenProvider.generateToken(202L, "distributor@agency.example");
    }

    @Test
    @DisplayName("LIFECYCLE: Customer creates booking -> Appears in distributor list -> Distributor accepts -> Customer sees CONFIRMED")
    void testRealCustomerBookingLifecycle_EndToEnd() throws Exception {
        // In-memory holder to simulate real database persistence
        AtomicReference<Booking> dbRecord = new AtomicReference<>();

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            if (b.getId() == null) {
                b.setId(777L);
                b.setBookingDate(LocalDateTime.now());
            }
            dbRecord.set(b);
            return b;
        });

        when(bookingRepository.findById(777L)).thenAnswer(inv -> Optional.ofNullable(dbRecord.get()));
        when(bookingRepository.findByIdWithLock(777L)).thenAnswer(inv -> Optional.ofNullable(dbRecord.get()));
        when(bookingRepository.findByDistributorIdWithDetails(1L)).thenAnswer(inv ->
                dbRecord.get() != null ? List.of(dbRecord.get()) : Collections.emptyList());
        when(bookingRepository.findByCustomerIdWithDetails(101L)).thenAnswer(inv ->
                dbRecord.get() != null ? List.of(dbRecord.get()) : Collections.emptyList());

        // ── STEP 1: Customer submits real booking via POST /api/bookings ──
        BookingRequestDTO requestDTO = new BookingRequestDTO();
        requestDTO.setCylinderCategory("DOMESTIC_14_2");
        requestDTO.setCapacityKg(14.2);
        requestDTO.setQuantity(1);
        requestDTO.setDeliveryAddress("Flat 402, Sunshine Heights, Sector 18, Vashi");
        requestDTO.setDistributorId(1L);
        requestDTO.setPaymentMethod("CASH_ON_DELIVERY");

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(777))
                .andExpect(jsonPath("$.customerId").value(101))
                .andExpect(jsonPath("$.distributorId").value(1))
                .andExpect(jsonPath("$.distributorName").value("Apex Gas Agency"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.cylinderCategory").value("DOMESTIC_14_2"))
                .andExpect(jsonPath("$.quantity").value(1))
                .andExpect(jsonPath("$.totalAmount").value(853.0))
                .andExpect(jsonPath("$.subsidyAmount").value(200.0))
                .andExpect(jsonPath("$.netAmount").value(653.0));

        // ── STEP 2: Authenticate as distributor, fetch GET /api/bookings/distributor/1 ──
        mockMvc.perform(get("/api/bookings/distributor/1")
                        .header("Authorization", "Bearer " + distributorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(777))
                .andExpect(jsonPath("$[0].customerId").value(101))
                .andExpect(jsonPath("$[0].customerName").value("Arman Shak"))
                .andExpect(jsonPath("$[0].distributorId").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        // ── STEP 3: Distributor accepts booking via PATCH /api/bookings/777/status -> CONFIRMED ──
        mockMvc.perform(patch("/api/bookings/777/status")
                        .header("Authorization", "Bearer " + distributorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(777))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // ── STEP 4: Authenticate as customer, verify updated status is CONFIRMED ──
        mockMvc.perform(get("/api/bookings/customer/101")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(777))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("RESOLUTION: When customer omits distributorId, resolves from customer's saved agency/past bookings")
    void testCustomerBookingResolvesFromSavedAgencyWhenOmitted() throws Exception {
        // Setup past booking for this customer associated with Apex Gas Agency
        Booking pastBooking = new Booking();
        pastBooking.setId(10L);
        pastBooking.setCustomer(customerUser);
        pastBooking.setDistributor(apexAgency);
        pastBooking.setStatus("DELIVERED");
        pastBooking.setBookingDate(LocalDateTime.now().minusDays(5));

        when(bookingRepository.findByCustomerIdWithDetails(101L)).thenReturn(List.of(pastBooking));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(888L);
            return b;
        });

        BookingRequestDTO requestDTO = new BookingRequestDTO();
        requestDTO.setCylinderCategory("DOMESTIC_14_2");
        requestDTO.setCapacityKg(14.2);
        requestDTO.setQuantity(1);
        requestDTO.setDeliveryAddress("Sector 17, Vashi");
        // distributorId is deliberately null to test resolution from past relationship
        requestDTO.setDistributorId(null);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(888))
                .andExpect(jsonPath("$.customerId").value(101))
                .andExpect(jsonPath("$.distributorId").value(1))
                .andExpect(jsonPath("$.distributorName").value("Apex Gas Agency"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("REJECTION: When no valid agency can be determined, reject with 400 Bad Request (no silent Apex assignment)")
    void testCustomerBookingRejectsWhenAgencyCannotBeDetermined() throws Exception {
        // New customer with no past bookings and no distributor membership
        when(bookingRepository.findByCustomerIdWithDetails(101L)).thenReturn(Collections.emptyList());
        when(distributorMembershipRepository.findByUserId(101L)).thenReturn(Collections.emptyList());
        // Customer has no matching PIN code in profile
        customerUser.setPincode(null);

        BookingRequestDTO requestDTO = new BookingRequestDTO();
        requestDTO.setCylinderCategory("DOMESTIC_14_2");
        requestDTO.setCapacityKg(14.2);
        requestDTO.setQuantity(1);
        requestDTO.setDeliveryAddress("Sector 17, Vashi");
        // Missing distributorId
        requestDTO.setDistributorId(null);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Distributor ID is required")));
    }

    @Test
    @DisplayName("SECURITY: Customer selecting an inactive distributor agency receives 400 Bad Request")
    void testCustomerBookingRejectsInactiveDistributor() throws Exception {
        Distributor inactiveAgency = new Distributor();
        inactiveAgency.setId(99L);
        inactiveAgency.setAgencyName("Suspended Agency");
        inactiveAgency.setStatus("SUSPENDED");
        when(distributorRepository.findById(99L)).thenReturn(Optional.of(inactiveAgency));

        BookingRequestDTO requestDTO = new BookingRequestDTO();
        requestDTO.setCylinderCategory("DOMESTIC_14_2");
        requestDTO.setCapacityKg(14.2);
        requestDTO.setQuantity(1);
        requestDTO.setDeliveryAddress("Sector 17, Vashi");
        requestDTO.setDistributorId(99L);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("is not active")));
    }

    @Test
    @DisplayName("VALIDATION: Customer selecting a non-existent distributor ID receives 400 Bad Request")
    void testCustomerBookingRejectsNonExistentDistributor() throws Exception {
        when(distributorRepository.findById(9999L)).thenReturn(Optional.empty());

        BookingRequestDTO requestDTO = new BookingRequestDTO();
        requestDTO.setCylinderCategory("DOMESTIC_14_2");
        requestDTO.setCapacityKg(14.2);
        requestDTO.setQuantity(1);
        requestDTO.setDeliveryAddress("Sector 17, Vashi");
        requestDTO.setDistributorId(9999L);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("not found")));
    }
}
