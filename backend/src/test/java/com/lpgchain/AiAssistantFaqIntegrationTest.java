package com.lpgchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.AiChatRequestDTO;
import com.lpgchain.entity.*;
import com.lpgchain.repository.*;
import com.lpgchain.security.JwtTokenProvider;
import com.lpgchain.service.FaqIntentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AiAssistantFaqIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private FaqIntentService faqIntentService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserRoleRepository userRoleRepository;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private CustomerDocumentRepository customerDocumentRepository;

    @MockBean
    private ComplaintRepository complaintRepository;

    private User testCustomer;
    private String customerToken;

    @BeforeEach
    void setUp() {
        testCustomer = new User();
        testCustomer.setId(101L);
        testCustomer.setEmail("citizen@example.com");
        testCustomer.setName("Aryan Customer");

        Role role = new Role(1L, "CUSTOMER");
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(testCustomer));
        when(userRepository.findById(101L)).thenReturn(Optional.of(testCustomer));
        when(userRoleRepository.findByUser(testCustomer)).thenReturn(List.of(new UserRole(testCustomer, role)));

        customerToken = jwtTokenProvider.generateToken(101L, "citizen@example.com", "CUSTOMER");

        // Ensure support phone is configured by default for tests
        ReflectionTestUtils.setField(faqIntentService, "supportPhoneConfig", "1800-233-3555");
    }

    // -------------------------------------------------------------
    // 1. All 8 Supported FAQ Categories (General FAQs)
    // -------------------------------------------------------------
    @Test
    @DisplayName("FAQ 1: How to book an LPG cylinder")
    void testFaqHowToBook() throws Exception {
        AiChatRequestDTO req = new AiChatRequestDTO("How to book an LPG cylinder?");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("Domestic 14.2 kg")))
                .andExpect(jsonPath("$.reply", containsString("₹853.00")))
                .andExpect(jsonPath("$.actionTab").value("book-cylinder"))
                .andExpect(jsonPath("$.suggestions", hasItem("Check my booking status")));
    }

    @Test
    @DisplayName("FAQ 2: How to check booking status (General FAQ)")
    void testFaqCheckBookingStatusGeneral() throws Exception {
        AiChatRequestDTO req = new AiChatRequestDTO("How do I check booking status?");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("Bookings")))
                .andExpect(jsonPath("$.reply", containsString("PENDING")))
                .andExpect(jsonPath("$.actionTab").value("bookings"));
    }

    @Test
    @DisplayName("FAQ 3: How to track a dispatched delivery")
    void testFaqTrackDelivery() throws Exception {
        AiChatRequestDTO req = new AiChatRequestDTO("How to track a dispatched delivery?");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("Track Delivery")))
                .andExpect(jsonPath("$.reply", containsString("Delivery OTP Verification")))
                .andExpect(jsonPath("$.actionTab").value("track-delivery"));
    }

    @Test
    @DisplayName("FAQ 4: Payment methods and payment-status help")
    void testFaqPaymentMethods() throws Exception {
        AiChatRequestDTO req = new AiChatRequestDTO("What payment methods are available?");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("Cash on Delivery")))
                .andExpect(jsonPath("$.reply", containsString("DBTL / PAHAL Subsidy")))
                .andExpect(jsonPath("$.actionTab").value("billing"));
    }

    @Test
    @DisplayName("FAQ 5: Required KYC documents and document-upload help")
    void testFaqKycRequirements() throws Exception {
        AiChatRequestDTO req = new AiChatRequestDTO("What are the required KYC documents?");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("Aadhaar Card")))
                .andExpect(jsonPath("$.reply", containsString("Ration Card")))
                .andExpect(jsonPath("$.reply", containsString("Bank Passbook")))
                .andExpect(jsonPath("$.actionTab").value("kyc-documents"));
    }

    @Test
    @DisplayName("FAQ 6: How to raise a complaint")
    void testFaqRaiseComplaint() throws Exception {
        AiChatRequestDTO req = new AiChatRequestDTO("How do I raise a complaint?");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("Help & Support")))
                .andExpect(jsonPath("$.reply", containsString("Gas Leakage")))
                .andExpect(jsonPath("$.actionTab").value("support"));
    }

    @Test
    @DisplayName("FAQ 7: LPG safety guidelines and emergency guidance")
    void testFaqSafetyGuidelines() throws Exception {
        AiChatRequestDTO req = new AiChatRequestDTO("LPG safety guidelines and emergency protocol");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("1906")))
                .andExpect(jsonPath("$.reply", containsString("regulator knob")))
                .andExpect(jsonPath("$.reply", containsString("14.2 kg")));
    }

    @Test
    @DisplayName("FAQ 8: How to contact customer support")
    void testFaqContactSupport() throws Exception {
        AiChatRequestDTO req = new AiChatRequestDTO("How to contact customer support?");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("1800-233-3555")))
                .andExpect(jsonPath("$.supportPhone").value("1800-233-3555"))
                .andExpect(jsonPath("$.reply", containsString("1906")));
    }

    // -------------------------------------------------------------
    // 2. Different Phrasings and Synonym Variations
    // -------------------------------------------------------------
    @Test
    @DisplayName("Phrasing variation: 'steps to order gas refill' matches booking how-to")
    void testPhrasingVariationBooking() throws Exception {
        AiChatRequestDTO req = new AiChatRequestDTO("can you tell me the steps to order gas refill?");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolUsed").value("BOOKING_HOWTO"))
                .andExpect(jsonPath("$.reply", containsString("Domestic 14.2 kg")));
    }

    @Test
    @DisplayName("Phrasing variation: 'gas leak emergency 1906' matches safety guidelines")
    void testPhrasingVariationGasLeak() throws Exception {
        AiChatRequestDTO req = new AiChatRequestDTO("I smell gas leak what emergency protocol to follow?");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolUsed").value("SAFETY_GUIDELINES"))
                .andExpect(jsonPath("$.reply", containsString("1906")));
    }

    // -------------------------------------------------------------
    // 3. Ambiguous and Out-of-Scope Fallbacks
    // -------------------------------------------------------------
    @Test
    @DisplayName("Ambiguous short greeting prompts clarifying options")
    void testAmbiguousGreeting() throws Exception {
        AiChatRequestDTO req = new AiChatRequestDTO("Hello");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolUsed").value("AMBIGUOUS_GREETING"))
                .andExpect(jsonPath("$.reply", containsString("How can I assist you today?")))
                .andExpect(jsonPath("$.suggestions", hasSize(greaterThanOrEqualTo(4))));
    }

    @Test
    @DisplayName("Out-of-scope question offers configured support phone number")
    void testOutOfScopeWithConfiguredPhone() throws Exception {
        AiChatRequestDTO req = new AiChatRequestDTO("What is the capital of France?");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolUsed").value("OUT_OF_SCOPE"))
                .andExpect(jsonPath("$.reply", containsString("outside the scope of LPG-Chain services")))
                .andExpect(jsonPath("$.reply", containsString("1800-233-3555")))
                .andExpect(jsonPath("$.supportPhone").value("1800-233-3555"));
    }

    @Test
    @DisplayName("Missing support phone configuration returns exact fallback text")
    void testMissingSupportPhoneFallback() throws Exception {
        // Temporarily clear support phone configuration
        ReflectionTestUtils.setField(faqIntentService, "supportPhoneConfig", "");

        AiChatRequestDTO req = new AiChatRequestDTO("What is the weather forecast in Tokyo?");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolUsed").value("OUT_OF_SCOPE"))
                .andExpect(jsonPath("$.reply").value("I couldn't find an answer to that question. Please contact the LPG-Chain support team for assistance."))
                .andExpect(jsonPath("$.supportPhone").doesNotExist());
    }

    // -------------------------------------------------------------
    // 4. Account-Specific Questions With and Without Authentication
    // -------------------------------------------------------------
    @Test
    @DisplayName("Account question without authentication prompts for login without fabricating data")
    void testAccountQuestionUnauthenticated() throws Exception {
        AiChatRequestDTO req = new AiChatRequestDTO("Where is my booking status?");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolUsed").value("unauthenticatedAccountQuery"))
                .andExpect(jsonPath("$.reply", containsString("please log in to your LPG-Chain account")))
                .andExpect(jsonPath("$.data.authenticated").value(false));
    }

    @Test
    @DisplayName("Authenticated customer question retrieves real database booking records")
    void testAuthenticatedCustomerBookingStatus() throws Exception {
        Booking booking = new Booking();
        booking.setId(888L);
        booking.setBookingNumber("BK-888");
        booking.setCustomer(testCustomer);
        booking.setCylinderCategory("DOMESTIC_14_2");
        booking.setQuantity(1);
        booking.setStatus("CONFIRMED");
        booking.setPaymentStatus("PAID");
        booking.setNetAmount(653.0);
        booking.setBookingDate(LocalDateTime.now());

        when(bookingRepository.findByCustomerIdWithDetails(101L)).thenReturn(List.of(booking));

        AiChatRequestDTO req = new AiChatRequestDTO("Where is my booking?");

        mockMvc.perform(post("/api/ai/assistant/chat")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolUsed").value("getMyBookings"))
                .andExpect(jsonPath("$.reply", containsString("BK-888")))
                .andExpect(jsonPath("$.reply", containsString("CONFIRMED")))
                .andExpect(jsonPath("$.reply", containsString("Aryan Customer")));
    }
}
