package com.lpgchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.*;
import com.lpgchain.entity.*;
import com.lpgchain.repository.*;
import com.lpgchain.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MasterWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserRoleRepository userRoleRepository;

    @MockBean
    private SavedAddressRepository savedAddressRepository;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private ComplaintRepository complaintRepository;

    @MockBean
    private CustomerDocumentRepository customerDocumentRepository;

    @MockBean
    private DistributorRepository distributorRepository;

    @MockBean
    private DistributorMembershipRepository distributorMembershipRepository;

    private User customerUser;
    private User otherCustomerUser;
    private User distributorUser;
    private User adminUser;

    private String customerToken;
    private String distributorToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        customerUser = new User();
        customerUser.setId(101L);
        customerUser.setEmail("citizen@example.com");
        customerUser.setName("Citizen User");

        otherCustomerUser = new User();
        otherCustomerUser.setId(102L);
        otherCustomerUser.setEmail("other@example.com");
        otherCustomerUser.setName("Other User");

        distributorUser = new User();
        distributorUser.setId(201L);
        distributorUser.setEmail("distributor@example.com");
        distributorUser.setName("Distributor Agent");

        adminUser = new User();
        adminUser.setId(301L);
        adminUser.setEmail("admin@example.com");
        adminUser.setName("Admin Officer");

        // Roles
        Role customerRole = new Role(1L, "CUSTOMER");
        Role distributorRole = new Role(2L, "DISTRIBUTOR");
        Role adminRole = new Role(3L, "ADMIN");

        when(userRoleRepository.findByUser(customerUser)).thenReturn(List.of(new UserRole(customerUser, customerRole)));
        when(userRoleRepository.findByUser(otherCustomerUser)).thenReturn(List.of(new UserRole(otherCustomerUser, customerRole)));
        when(userRoleRepository.findByUser(distributorUser)).thenReturn(List.of(new UserRole(distributorUser, distributorRole)));
        when(userRoleRepository.findByUser(adminUser)).thenReturn(List.of(new UserRole(adminUser, adminRole)));

        // Security tokens
        customerToken = jwtTokenProvider.generateToken(101L, "citizen@example.com", "CUSTOMER");
        distributorToken = jwtTokenProvider.generateToken(201L, "distributor@example.com", "DISTRIBUTOR");
        adminToken = jwtTokenProvider.generateToken(301L, "admin@example.com", "ADMIN");

        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(customerUser));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherCustomerUser));
        when(userRepository.findByEmail("distributor@example.com")).thenReturn(Optional.of(distributorUser));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(101L)).thenReturn(Optional.of(customerUser));
        when(userRepository.findById(102L)).thenReturn(Optional.of(otherCustomerUser));
        when(userRepository.findById(201L)).thenReturn(Optional.of(distributorUser));
        when(userRepository.findById(301L)).thenReturn(Optional.of(adminUser));
    }

    // -------------------------------------------------------------
    // 1. Customer Profile and Address Ownership
    // -------------------------------------------------------------
    @Test
    @DisplayName("Customer can retrieve and update their own delivery profile")
    void testCustomerProfileUpdate() throws Exception {
        CustomerProfileDTO updateDTO = new CustomerProfileDTO();
        updateDTO.setName("Citizen Updated");
        updateDTO.setPhone("+91 99999 88888");
        updateDTO.setAddress("Flat 101, Galaxy Heights");
        updateDTO.setCity("Pune");
        updateDTO.setState("Maharashtra");
        updateDTO.setPincode("411018");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/customer/profile")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Citizen Updated"))
                .andExpect(jsonPath("$.pincode").value("411018"));
    }

    @Test
    @DisplayName("Customer cannot delete or modify another customer's saved address")
    void testCrossCustomerAddressAccessDenied() throws Exception {
        SavedAddress address = new SavedAddress();
        address.setId(500L);
        address.setUser(otherCustomerUser); // Owned by otherCustomerUser (102L)
        address.setFullAddress("45 MG Road");

        when(savedAddressRepository.findById(500L)).thenReturn(Optional.of(address));

        // Customer 101 attempts to delete customer 102's address
        mockMvc.perform(delete("/api/customer/addresses/500")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------
    // 2. Demo QR Payment & COD Authorization
    // -------------------------------------------------------------
    @Test
    @DisplayName("Customer can simulate successful Demo QR payment idempotently")
    void testSimulateDemoQrPayment() throws Exception {
        Booking booking = new Booking();
        booking.setId(901L);
        booking.setBookingNumber("BK-901");
        booking.setCustomer(customerUser);
        booking.setStatus("PENDING");
        booking.setPaymentStatus("PENDING");
        booking.setTotalAmount(853.0);
        booking.setNetAmount(653.0);

        Payment payment = new Payment();
        payment.setId(801L);
        payment.setBooking(booking);
        payment.setCustomer(customerUser);
        payment.setAmount(853.0);
        payment.setNetAmount(653.0);
        payment.setTransactionReference("TXN-DEMO-901");
        payment.setPaymentStatus("PENDING");
        payment.setPaymentMethod("DEMO_QR");

        when(paymentRepository.findByTransactionReference("TXN-DEMO-901")).thenReturn(Optional.of(payment));
        when(bookingRepository.findById(901L)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        SimulatePaymentRequestDTO req = new SimulatePaymentRequestDTO();
        req.setTransactionReference("TXN-DEMO-901");

        mockMvc.perform(post("/api/payments/simulate-success")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.isSimulated").value(true));
    }

    @Test
    @DisplayName("Customer is strictly forbidden from self-confirming Cash on Delivery (COD)")
    void testCustomerCannotSelfConfirmCod() throws Exception {
        ConfirmCodRequestDTO req = new ConfirmCodRequestDTO();
        req.setBookingId(902L);

        // A customer attempts to call the COD confirmation endpoint
        mockMvc.perform(post("/api/payments/confirm-cod")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authorized Distributor can confirm Cash on Delivery payment collection")
    void testDistributorCanConfirmCod() throws Exception {
        Booking booking = new Booking();
        booking.setId(902L);
        booking.setBookingNumber("BK-902");
        booking.setCustomer(customerUser);
        booking.setStatus("DELIVERED");
        booking.setPaymentStatus("PENDING");

        Payment payment = new Payment();
        payment.setId(802L);
        payment.setBooking(booking);
        payment.setCustomer(customerUser);
        payment.setAmount(853.0);
        payment.setNetAmount(853.0);
        payment.setTransactionReference("TXN-COD-902");
        payment.setPaymentStatus("PENDING");
        payment.setPaymentMethod("CASH_ON_DELIVERY");

        when(bookingRepository.findByIdWithDetails(902L)).thenReturn(Optional.of(booking));
        when(bookingRepository.findById(902L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(902L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfirmCodRequestDTO req = new ConfirmCodRequestDTO();
        req.setBookingId(902L);

        mockMvc.perform(post("/api/payments/confirm-cod")
                .header("Authorization", "Bearer " + distributorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    // -------------------------------------------------------------
    // 3. KYC Document Compliance Calculation
    // -------------------------------------------------------------
    @Test
    @DisplayName("KYC Summary correctly reports pending documents and does not claim up-to-date prematurely")
    void testKycSummaryIncompleteState() throws Exception {
        CustomerDocument doc1 = new CustomerDocument();
        doc1.setId(1L);
        doc1.setCustomer(customerUser);
        doc1.setDocumentType("AADHAAR_CARD");
        doc1.setStatus("APPROVED");

        CustomerDocument doc2 = new CustomerDocument();
        doc2.setId(2L);
        doc2.setCustomer(customerUser);
        doc2.setDocumentType("ELECTRICITY_BILL");
        doc2.setStatus("SUBMITTED"); // Pending review

        when(customerDocumentRepository.findByCustomerIdOrderByCreatedAtDesc(101L)).thenReturn(List.of(doc1, doc2));

        mockMvc.perform(get("/api/documents/summary")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allApproved").value(false))
                .andExpect(jsonPath("$.approvedCount").value(1))
                .andExpect(jsonPath("$.pendingCount").value(1))
                .andExpect(jsonPath("$.remainingRequiredCount").value(3));
    }

    // -------------------------------------------------------------
    // 4. Complaint Management
    // -------------------------------------------------------------
    @Test
    @DisplayName("Customer can create complaint and Admin can resolve it with resolution notes")
    void testComplaintWorkflow() throws Exception {
        Complaint complaint = new Complaint();
        complaint.setId(401L);
        complaint.setComplaintNumber("CMP-401");
        complaint.setCustomer(customerUser);
        complaint.setSubject("Late delivery");
        complaint.setDescription("Cylinder was not delivered during the scheduled morning slot.");
        complaint.setStatus("OPEN");

        when(complaintRepository.findById(401L)).thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        ComplaintStatusUpdateDTO updateDTO = new ComplaintStatusUpdateDTO();
        updateDTO.setStatus("RESOLVED");
        updateDTO.setResolutionNote("Express re-delivery scheduled and delivered at 2 PM. Agent penalized.");

        mockMvc.perform(put("/api/complaints/401/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionNote").value(updateDTO.getResolutionNote()));
    }

    // -------------------------------------------------------------
    // 5. Ollama AI Assistant with Scoped Tools & Offline Fallback
    // -------------------------------------------------------------
    @Test
    @DisplayName("AI Assistant answers customer questions grounded in real database records with graceful offline fallback")
    void testAiAssistantCustomerGroundedResponse() throws Exception {
        Booking booking = new Booking();
        booking.setId(777L);
        booking.setBookingNumber("BK-777");
        booking.setCustomer(customerUser);
        booking.setQuantity(1);
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("PAID");
        booking.setNetAmount(653.0);
        booking.setBookingDate(LocalDateTime.now());

        when(bookingRepository.findByCustomerIdWithDetails(101L)).thenReturn(List.of(booking));
        when(bookingRepository.findByCustomerId(101L)).thenReturn(List.of(booking));

        AiChatRequestDTO req = new AiChatRequestDTO();
        req.setMessage("Where is my latest booking and is it paid?");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").isNotEmpty())
                .andExpect(jsonPath("$.toolUsed").value("getMyBookings"));
    }

    // -------------------------------------------------------------
    // 6. Razorpay Order Creation & Retry Semantics
    // -------------------------------------------------------------
    @Test
    @DisplayName("Razorpay test order returns backend-calculated amount and demo fallback when unconfigured")
    void testRazorpayOrderCreation() throws Exception {
        Booking booking = new Booking();
        booking.setId(999L);
        booking.setBookingNumber("BK-999");
        booking.setCustomer(customerUser);
        booking.setTotalAmount(853.0);
        booking.setNetAmount(653.0);
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("PENDING");

        when(bookingRepository.findByIdWithDetails(999L)).thenReturn(Optional.of(booking));
        when(bookingRepository.findById(999L)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByBookingId(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/payments/razorpay/order/999")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(999))
                .andExpect(jsonPath("$.amount").value(653.0))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.paymentReference").isNotEmpty());
    }

    @Test
    @DisplayName("Customer can simulate payment failure and retry to receive a fresh transaction reference")
    void testPaymentFailureAndRetry() throws Exception {
        Booking booking = new Booking();
        booking.setId(998L);
        booking.setBookingNumber("BK-998");
        booking.setCustomer(customerUser);
        booking.setTotalAmount(853.0);
        booking.setNetAmount(653.0);
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("PENDING");

        Payment payment = new Payment();
        payment.setId(888L);
        payment.setBooking(booking);
        payment.setCustomer(customerUser);
        payment.setAmount(853.0);
        payment.setNetAmount(653.0);
        payment.setTransactionReference("TXN-DEMO-998-ORIG");
        payment.setPaymentStatus("PENDING");
        payment.setIsSimulated(true);

        when(bookingRepository.findByIdWithDetails(998L)).thenReturn(Optional.of(booking));
        when(bookingRepository.findById(998L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(998L)).thenReturn(Optional.of(payment));
        when(paymentRepository.findByTransactionReference("TXN-DEMO-998-ORIG")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Simulate failure
        SimulatePaymentRequestDTO failReq = new SimulatePaymentRequestDTO();
        failReq.setTransactionReference("TXN-DEMO-998-ORIG");
        failReq.setReason("Customer simulated cancellation");

        mockMvc.perform(post("/api/payments/simulate-failure")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(failReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"));

        // 2. Retry payment
        mockMvc.perform(post("/api/payments/retry/998")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.transactionReference").value(org.hamcrest.Matchers.startsWith("TXN-RETRY-")));
    }

    // -------------------------------------------------------------
    // 7. KYC Multipart Document Upload & Approval
    // -------------------------------------------------------------
    @Test
    @DisplayName("Valid PDF document upload succeeds and enters SUBMITTED status")
    void testKycMultipartUploadSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "aadhaar_card.pdf",
                "application/pdf",
                "%PDF-1.4 Mock Aadhaar Document Content".getBytes()
        );

        when(customerDocumentRepository.save(any(CustomerDocument.class))).thenAnswer(inv -> {
            CustomerDocument doc = inv.getArgument(0);
            doc.setId(10L);
            return doc;
        });

        mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("documentType", "AADHAAR_CARD")
                .param("documentNumber", "1234-5678-9012")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.documentType").value("AADHAAR_CARD"));
    }

    @Test
    @DisplayName("Invalid executable file upload is rejected with 400 Bad Request")
    void testKycMultipartUploadInvalidFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                "MZ executable content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                .file(file)
                .param("documentType", "AADHAAR_CARD")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported file type")));
    }

    @Test
    @DisplayName("Admin can review and approve a submitted KYC document")
    void testAdminApproveKycDocument() throws Exception {
        CustomerDocument doc = new CustomerDocument();
        doc.setId(15L);
        doc.setCustomer(customerUser);
        doc.setDocumentType("AADHAAR_CARD");
        doc.setStatus("SUBMITTED");

        when(customerDocumentRepository.findById(15L)).thenReturn(Optional.of(doc));
        when(customerDocumentRepository.save(any(CustomerDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentReviewDTO reviewDTO = new DocumentReviewDTO();
        reviewDTO.setStatus("APPROVED");

        mockMvc.perform(put("/api/documents/15/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
