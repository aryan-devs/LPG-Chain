package com.lpgchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.BookingResponseDTO;
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
import com.lpgchain.service.BookingService;
import com.lpgchain.service.BookingService.StatusUpdateResult;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DistributorAcceptQueueWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private BookingService bookingService;

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

    private User distributorUserA;
    private User distributorUserB;
    private User customerUser;
    private Distributor distributorOrgA;
    private Distributor distributorOrgB;
    private String distributorAToken;
    private String distributorBToken;

    private Booking pendingBookingA;
    private Booking pendingBookingB;
    private Booking confirmedBookingA;
    private Booking cancelledBookingA;

    @BeforeEach
    void setUp() {
        Role distributorRole = new Role(2L, "DISTRIBUTOR");
        Role customerRole = new Role(1L, "CUSTOMER");

        distributorUserA = new User(501L, "Distributor User A", "distributorA@agency.example", "hash");
        distributorUserB = new User(502L, "Distributor User B", "distributorB@agency.example", "hash");
        customerUser = new User(101L, "Customer Test", "customer@example.com", "hash");

        when(userRepository.findByEmail(distributorUserA.getEmail())).thenReturn(Optional.of(distributorUserA));
        when(userRepository.findByEmail(distributorUserB.getEmail())).thenReturn(Optional.of(distributorUserB));
        when(userRepository.findByEmail(customerUser.getEmail())).thenReturn(Optional.of(customerUser));

        when(userRoleRepository.findByUser(distributorUserA)).thenReturn(List.of(new UserRole(distributorUserA, distributorRole)));
        when(userRoleRepository.findByUser(distributorUserB)).thenReturn(List.of(new UserRole(distributorUserB, distributorRole)));
        when(userRoleRepository.findByUser(customerUser)).thenReturn(List.of(new UserRole(customerUser, customerRole)));

        distributorOrgA = new Distributor(1L, "Sharma Bharat Gas Agency", "LIC-001", "9876543210", "Pune", "ACTIVE", "pass", LocalDateTime.now().plusDays(1), false);
        distributorOrgB = new Distributor(2L, "Apex Gas Agency", "LIC-002", "9876543211", "Mumbai", "ACTIVE", "pass", LocalDateTime.now().plusDays(1), false);

        DistributorMembership membershipA = new DistributorMembership(1L, distributorUserA, distributorOrgA, "STAFF", "ACTIVE");
        DistributorMembership membershipB = new DistributorMembership(2L, distributorUserB, distributorOrgB, "STAFF", "ACTIVE");

        when(distributorMembershipRepository.existsByUserIdAndDistributorId(501L, 1L)).thenReturn(true);
        when(distributorMembershipRepository.existsByUserIdAndDistributorId(502L, 2L)).thenReturn(true);
        when(distributorMembershipRepository.findByUserId(501L)).thenReturn(List.of(membershipA));
        when(distributorMembershipRepository.findByUserId(502L)).thenReturn(List.of(membershipB));

        pendingBookingA = new Booking(100L, customerUser, distributorOrgA, "Pune, MH", LocalDateTime.now(), 1, "PENDING");
        pendingBookingB = new Booking(200L, customerUser, distributorOrgB, "Mumbai, MH", LocalDateTime.now(), 1, "PENDING");
        confirmedBookingA = new Booking(300L, customerUser, distributorOrgA, "Pune, MH", LocalDateTime.now(), 1, "CONFIRMED");
        cancelledBookingA = new Booking(400L, customerUser, distributorOrgA, "Pune, MH", LocalDateTime.now(), 1, "CANCELLED");

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(pendingBookingA));
        when(bookingRepository.findByIdWithLock(100L)).thenReturn(Optional.of(pendingBookingA));

        when(bookingRepository.findById(200L)).thenReturn(Optional.of(pendingBookingB));
        when(bookingRepository.findByIdWithLock(200L)).thenReturn(Optional.of(pendingBookingB));

        when(bookingRepository.findById(300L)).thenReturn(Optional.of(confirmedBookingA));
        when(bookingRepository.findByIdWithLock(300L)).thenReturn(Optional.of(confirmedBookingA));

        when(bookingRepository.findById(400L)).thenReturn(Optional.of(cancelledBookingA));
        when(bookingRepository.findByIdWithLock(400L)).thenReturn(Optional.of(cancelledBookingA));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        distributorAToken = jwtTokenProvider.generateToken(501L, "distributorA@agency.example");
        distributorBToken = jwtTokenProvider.generateToken(502L, "distributorB@agency.example");
    }

    @Test
    @DisplayName("1. CORS preflight OPTIONS request for PATCH contains allowed origin and PATCH method")
    void testCorsPreflightOptionsForPatch() throws Exception {
        mockMvc.perform(options("/api/bookings/100/status")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(result -> {
                    String methods = result.getResponse().getHeader("Access-Control-Allow-Methods");
                    assertNotNull(methods);
                    assertTrue(methods.contains("PATCH"), "Allow-Methods must include PATCH");
                });
    }

    @Test
    @DisplayName("2. Successful acceptance of a valid pending booking -> 200 OK and CONFIRMED status")
    void testSuccessfulAcceptanceOfPendingBooking() throws Exception {
        mockMvc.perform(patch("/api/bookings/100/status")
                        .header("Authorization", "Bearer " + distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.distributorId").value(1));

        assertEquals("CONFIRMED", pendingBookingA.getStatus(), "Booking status must be updated to CONFIRMED");
    }

    @Test
    @DisplayName("3. Unauthorized distributor attempting to accept another agency's booking -> 403 Forbidden")
    void testUnauthorizedDistributorCannotAcceptOtherDistributorBooking() throws Exception {
        // Distributor User A attempts to accept booking 200 (assigned to Distributor Org B)
        mockMvc.perform(patch("/api/bookings/200/status")
                        .header("Authorization", "Bearer " + distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden: Booking is not assigned to your distributor organization"));

        assertEquals("PENDING", pendingBookingB.getStatus(), "Unassigned booking status must remain PENDING");
    }

    @Test
    @DisplayName("4. Accepting an already accepted booking -> 400 Bad Request with informative error")
    void testAcceptingAlreadyAcceptedBookingFails() throws Exception {
        mockMvc.perform(patch("/api/bookings/300/status")
                        .header("Authorization", "Bearer " + distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Booking #300 is already in CONFIRMED status."));
    }

    @Test
    @DisplayName("5. Accepting a rejected/cancelled booking -> 400 Bad Request")
    void testAcceptingCancelledBookingFails() throws Exception {
        mockMvc.perform(patch("/api/bookings/400/status")
                        .header("Authorization", "Bearer " + distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Transition from CANCELLED to CONFIRMED is not allowed."));
    }

    @Test
    @DisplayName("6. Invalid booking ID -> 404 Not Found with structured error body")
    void testInvalidBookingIdReturnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/bookings/999999/status")
                        .header("Authorization", "Bearer " + distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Booking not found with id: 999999"));
    }

    @Test
    @DisplayName("7. Rejection with reason workflow -> 200 OK and CANCELLED status")
    void testRejectionWithReasonWorkflow() throws Exception {
        mockMvc.perform(patch("/api/bookings/100/status")
                        .header("Authorization", "Bearer " + distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "CANCELLED",
                                "reason", "Out of physical stock at depot"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertEquals("CANCELLED", pendingBookingA.getStatus(), "Booking status must be updated to CANCELLED");
    }

    @Test
    @DisplayName("8. Delivery progression integrity: Acceptance does not prematurely dispatch to OUT_FOR_DELIVERY or DELIVERED")
    void testAcceptanceDoesNotPrematurelyDispatch() throws Exception {
        mockMvc.perform(patch("/api/bookings/100/status")
                        .header("Authorization", "Bearer " + distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertEquals("CONFIRMED", pendingBookingA.getStatus());
        // Verify illegal jump directly to DELIVERED from PENDING is rejected
        Booking freshPending = new Booking(105L, customerUser, distributorOrgA, "Pune", LocalDateTime.now(), 1, "PENDING");
        when(bookingRepository.findById(105L)).thenReturn(Optional.of(freshPending));
        when(bookingRepository.findByIdWithLock(105L)).thenReturn(Optional.of(freshPending));

        mockMvc.perform(patch("/api/bookings/105/status")
                        .header("Authorization", "Bearer " + distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DELIVERED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transition from PENDING to DELIVERED is not allowed."));
    }

    @Test
    @DisplayName("9. Missing status in request body -> 400 Bad Request")
    void testMissingStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/bookings/100/status")
                        .header("Authorization", "Bearer " + distributorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Field 'status' is required."));
    }
}
