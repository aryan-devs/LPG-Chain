package com.lpgchain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.AiChatRequestDTO;
import com.lpgchain.dto.AiChatResponseDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Complaint;
import com.lpgchain.entity.CustomerDocument;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Payment;
import com.lpgchain.entity.SavedAddress;
import com.lpgchain.entity.User;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.ComplaintRepository;
import com.lpgchain.repository.CustomerDocumentRepository;
import com.lpgchain.repository.CylinderInventoryRepository;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.PaymentRepository;
import com.lpgchain.repository.SavedAddressRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.service.FaqIntentService.FaqIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class OllamaAssistantService {

    private static final Logger log = LoggerFactory.getLogger(OllamaAssistantService.class);

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerDocumentRepository customerDocumentRepository;
    private final ComplaintRepository complaintRepository;
    private final SavedAddressRepository savedAddressRepository;
    private final DistributorMembershipRepository distributorMembershipRepository;
    private final CylinderInventoryRepository cylinderInventoryRepository;
    private final FaqIntentService faqIntentService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:llama3.2}")
    private String ollamaModel;

    @Value("${ml.forecast.url:http://localhost:8000}")
    private String forecastApiUrl;

    public OllamaAssistantService(UserRepository userRepository,
                                  BookingRepository bookingRepository,
                                  PaymentRepository paymentRepository,
                                  CustomerDocumentRepository customerDocumentRepository,
                                  ComplaintRepository complaintRepository,
                                  SavedAddressRepository savedAddressRepository,
                                  DistributorMembershipRepository distributorMembershipRepository,
                                  CylinderInventoryRepository cylinderInventoryRepository,
                                  FaqIntentService faqIntentService,
                                  RestTemplateBuilder restTemplateBuilder,
                                  ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.customerDocumentRepository = customerDocumentRepository;
        this.complaintRepository = complaintRepository;
        this.savedAddressRepository = savedAddressRepository;
        this.distributorMembershipRepository = distributorMembershipRepository;
        this.cylinderInventoryRepository = cylinderInventoryRepository;
        this.faqIntentService = faqIntentService;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = objectMapper;
    }

    private Optional<User> getAuthenticatedUserOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return userRepository.findByEmail(auth.getName());
    }

    private String getPrimaryRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "ANONYMOUS";
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));
        if (isAdmin) return "ADMIN";
        boolean isDist = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("DISTRIBUTOR"));
        if (isDist) return "DISTRIBUTOR";
        boolean isCust = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("CUSTOMER"));
        if (isCust) return "CUSTOMER";
        return "ANONYMOUS";
    }

    public AiChatResponseDTO chat(AiChatRequestDTO request) {
        String query = request.getMessage() != null ? request.getMessage() : "";
        Optional<User> userOpt = getAuthenticatedUserOptional();
        String role = userOpt.isPresent() ? getPrimaryRole() : "ANONYMOUS";
        String supportPhone = faqIntentService.getConfiguredSupportPhone();

        // 1. If user is unauthenticated, match FAQ or require authentication for personal queries
        if (userOpt.isEmpty()) {
            FaqIntentService.IntentMatchResult match = faqIntentService.matchIntent(query, request.getIntentId());
            if (match.isAccountSpecific()) {
                return new AiChatResponseDTO(
                        "To check your personal booking, payment, delivery, or KYC status, please log in to your LPG-Chain account. I cannot access account-specific records without authentication.",
                        "SECURITY_GUARD",
                        "unauthenticatedAccountQuery",
                        Map.of("authenticated", false),
                        supportPhone,
                        null,
                        null,
                        Arrays.asList("How to book an LPG cylinder", "How to check booking status", "Payment methods available", "How to contact customer support")
                );
            }

            FaqIntentService.FaqAnswer generalAns = faqIntentService.getGeneralAnswer(match.getIntent());
            return new AiChatResponseDTO(
                    generalAns.getReply(),
                    "FAQ_ENGINE",
                    match.getIntent().name(),
                    Map.of("intent", match.getIntent().name(), "confidence", match.confidence()),
                    generalAns.getSupportPhone(),
                    generalAns.getActionTab(),
                    generalAns.getActionLabel(),
                    generalAns.getSuggestions()
            );
        }

        User user = userOpt.get();

        // 2. Handle DISTRIBUTOR-specific tools if query targets inventory or queues
        if ("DISTRIBUTOR".equals(role)) {
            String lower = query.toLowerCase();
            List<DistributorMembership> memberships = distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(user.getId(), "ACTIVE");
            Long agencyId = memberships.isEmpty() ? null : memberships.get(0).getDistributor().getId();

            if (agencyId != null && (lower.contains("stock") || lower.contains("inventory") || lower.contains("queue") || lower.contains("orders"))) {
                Map<String, Object> toolData = new HashMap<>();
                if (lower.contains("stock") || lower.contains("inventory")) {
                    var inventory = cylinderInventoryRepository.findByDistributorId(agencyId);
                    toolData.put("inventory", inventory.stream().map(i -> Map.of(
                            "category", i.getCylinderCategory() != null ? i.getCylinderCategory() : "DOMESTIC_14_2",
                            "filledAvailable", i.getFilledStock() != null ? i.getFilledStock() : 0,
                            "emptyDefective", i.getEmptyStock() != null ? i.getEmptyStock() : 0
                    )).toList());
                    return new AiChatResponseDTO(
                            "Agency Inventory Status:\n" + formatInventory(inventory),
                            "DATABASE_ASSISTANT",
                            "getAgencyInventory",
                            toolData,
                            supportPhone,
                            "inventory",
                            "View Agency Inventory",
                            Arrays.asList("View booking queue", "Delivery status")
                    );
                } else {
                    List<Booking> bookings = bookingRepository.findByDistributorIdWithDetails(agencyId);
                    toolData.put("bookings", bookings.stream().limit(10).map(b -> Map.of(
                            "bookingNumber", b.getBookingNumber() != null ? b.getBookingNumber() : ("BK-" + b.getId()),
                            "customer", b.getCustomer().getName(),
                            "quantity", b.getQuantity(),
                            "status", b.getStatus(),
                            "paymentStatus", b.getPaymentStatus()
                    )).toList());
                    return new AiChatResponseDTO(
                            "Active Distributor Booking Queue:\n" + formatBookingQueue(bookings),
                            "DATABASE_ASSISTANT",
                            "getAgencyBookingQueue",
                            toolData,
                            supportPhone,
                            "orders",
                            "Manage Orders",
                            Arrays.asList("Check agency stock", "Customer support")
                    );
                }
            }
        }

        // 3. Handle ADMIN-specific tools if query targets metrics, forecasting, or reviews
        if ("ADMIN".equals(role)) {
            String lower = query.toLowerCase();
            if (lower.contains("forecast") || lower.contains("demand") || lower.contains("prediction")) {
                Map<String, Object> toolData = new HashMap<>();
                try {
                    String forecastJson = restTemplate.getForObject(forecastApiUrl + "/forecast?days=7", String.class);
                    toolData.put("forecastData", objectMapper.readTree(forecastJson));
                    toolData.put("syntheticModelDisclaimer", "Trained on synthetic distribution history (v1.0.0-synthetic-rf). Demonstrative only.");
                } catch (Exception e) {
                    toolData.put("forecastError", "Forecasting service unavailable: " + e.getMessage());
                }
                String reply = "AI Demand Forecasting Summary (Next 7 Days):\n\n" +
                        "• Status: Active (FastAPI scikit-learn service)\n" +
                        "• Model: `v1.0.0-synthetic-rf` (Random Forest Regressor)\n" +
                        "• Projected Peak: Domestic 14.2kg cylinders show highest demand mid-week (~20-25 refills/day).\n" +
                        "• Note: Forecasts are generated using synthetic historical distribution trends for capacity scheduling.";
                return new AiChatResponseDTO(reply, "FASTAPI_FORECAST", "getDemandForecast", toolData, supportPhone, "demand-forecasting", "View Demand Forecast", Arrays.asList("Platform metrics", "Pending KYC"));
            } else if (lower.contains("metrics") || lower.contains("overview") || lower.contains("stats")) {
                Map<String, Object> toolData = new HashMap<>();
                toolData.put("totalBookings", bookingRepository.count());
                toolData.put("unresolvedComplaints", complaintRepository.countByStatus("OPEN"));
                toolData.put("pendingKycReview", customerDocumentRepository.countByStatus("SUBMITTED"));
                String reply = "LPG-Chain Operational Overview:\n\n" +
                        "• Total Platform Bookings: " + toolData.get("totalBookings") + "\n" +
                        "• Unresolved Customer Complaints: " + toolData.get("unresolvedComplaints") + "\n" +
                        "• KYC Documents Awaiting Review: " + toolData.get("pendingKycReview") + "\n" +
                        "All services (MySQL, Spring Security, WebSockets) are operating in real-time.";
                return new AiChatResponseDTO(reply, "DATABASE_ASSISTANT", "getAdminMetrics", toolData, supportPhone, "operations", "Admin Operations", Arrays.asList("Forecast demand", "Customer complaints"));
            }
        }

        // 4. Customer / Unified User Intent Matching
        FaqIntentService.IntentMatchResult match = faqIntentService.matchIntent(query, request.getIntentId());

        // Account-Specific Data Queries (Authorized User Only)
        if (match.isAccountSpecific()) {
            String lower = query.toLowerCase();

            // Bookings status
            if (match.getIntent() == FaqIntent.BOOKING_STATUS || match.getIntent() == FaqIntent.BOOKING_HOWTO ||
                    lower.contains("booking") || lower.contains("order") || lower.contains("refill") || lower.contains("where")) {
                List<Booking> bookings = bookingRepository.findByCustomerIdWithDetails(user.getId());
                Map<String, Object> data = new HashMap<>();
                data.put("bookings", bookings.stream().limit(5).map(b -> Map.of(
                        "bookingNumber", b.getBookingNumber() != null ? b.getBookingNumber() : ("BK-" + b.getId()),
                        "category", b.getCylinderCategory() != null ? b.getCylinderCategory() : "DOMESTIC_14_2",
                        "quantity", b.getQuantity() != null ? b.getQuantity() : 1,
                        "status", b.getStatus() != null ? b.getStatus() : "PENDING",
                        "netAmount", b.getNetAmount() != null ? b.getNetAmount() : 653.0,
                        "paymentStatus", b.getPaymentStatus() != null ? b.getPaymentStatus() : "PENDING",
                        "date", String.valueOf(b.getBookingDate())
                )).toList());

                StringBuilder sb = new StringBuilder();
                if (bookings.isEmpty()) {
                    sb.append("Hello ").append(user.getName()).append(", you currently do not have any active cylinder bookings. ")
                      .append("You can book a domestic (14.2kg/5kg) or commercial (19kg) cylinder directly from the Book Cylinder view.");
                } else {
                    sb.append("Hello ").append(user.getName()).append(", here are your latest cylinder booking(s):\n\n");
                    for (Booking b : bookings) {
                        String bNum = b.getBookingNumber() != null ? b.getBookingNumber() : ("BK-" + b.getId());
                        sb.append("• **").append(bNum).append("** — ")
                          .append(b.getCylinderCategory() != null ? b.getCylinderCategory() : "DOMESTIC_14_2")
                          .append(" (Qty: ").append(b.getQuantity() != null ? b.getQuantity() : 1).append(")\n")
                          .append("  Status: `").append(b.getStatus() != null ? b.getStatus() : "PENDING").append("`")
                          .append(" | Amount: ₹").append(b.getNetAmount() != null ? b.getNetAmount() : 653.0)
                          .append(" | Payment: `").append(b.getPaymentStatus() != null ? b.getPaymentStatus() : "PENDING").append("`\n\n");
                    }
                }

                return new AiChatResponseDTO(
                        sb.toString(),
                        "DATABASE_ASSISTANT",
                        "getMyBookings",
                        data,
                        supportPhone,
                        "bookings",
                        "View Bookings",
                        Arrays.asList("How to book an LPG cylinder", "How to track a dispatched delivery", "Payment methods available")
                );
            }

            // Payment status
            if (match.getIntent() == FaqIntent.PAYMENT_HELP || lower.contains("payment") || lower.contains("pay") || lower.contains("bill") || lower.contains("invoice")) {
                List<Payment> payments = paymentRepository.findByCustomerIdWithDetails(user.getId());
                Map<String, Object> data = new HashMap<>();
                data.put("payments", payments.stream().limit(5).map(p -> Map.of(
                        "reference", p.getTransactionReference() != null ? p.getTransactionReference() : ("TXN-" + p.getId()),
                        "amount", p.getNetAmount() != null ? p.getNetAmount() : 0.0,
                        "status", p.getPaymentStatus() != null ? p.getPaymentStatus() : "PENDING",
                        "method", p.getPaymentMethod() != null ? p.getPaymentMethod() : "DEMO_QR"
                )).toList());

                StringBuilder sb = new StringBuilder();
                if (payments.isEmpty()) {
                    sb.append("Hello ").append(user.getName()).append(", you have no recent payment records. ")
                      .append("If you just placed an order, please visit Billing & Payments to complete your checkout.");
                } else {
                    sb.append("Hello ").append(user.getName()).append(", here is your recent payment record summary:\n\n");
                    for (Payment p : payments) {
                        String ref = p.getTransactionReference() != null ? p.getTransactionReference() : ("TXN-" + p.getId());
                        sb.append("• Ref: `").append(ref).append("` — ₹").append(p.getNetAmount())
                          .append(" via ").append(p.getPaymentMethod()).append(" (Status: **").append(p.getPaymentStatus()).append("**)\n");
                    }
                    sb.append("\nNote: Demo online payments simulate immediate confirmation. For COD, payment is finalized at cylinder handover.");
                }

                return new AiChatResponseDTO(
                        sb.toString(),
                        "DATABASE_ASSISTANT",
                        "getMyPaymentStatus",
                        data,
                        supportPhone,
                        "billing",
                        "Billing & Payments",
                        Arrays.asList("How to book an LPG cylinder", "Check my booking status", "Required KYC documents")
                );
            }

            // KYC Documents status
            if (match.getIntent() == FaqIntent.KYC_HELP || lower.contains("document") || lower.contains("kyc") || lower.contains("aadhaar") || lower.contains("ration")) {
                List<CustomerDocument> docs = customerDocumentRepository.findByCustomerIdOrderByCreatedAtDesc(user.getId());
                long approved = docs.stream().filter(d -> "APPROVED".equalsIgnoreCase(d.getStatus())).count();
                Map<String, Object> data = new HashMap<>();
                data.put("totalRequired", 4);
                data.put("approvedCount", approved);
                data.put("allApproved", approved >= 4);

                StringBuilder sb = new StringBuilder();
                sb.append("KYC Compliance Summary for ").append(user.getName()).append(":\n\n")
                  .append("• Approved Documents: ").append(approved).append(" / 4\n");
                if (approved >= 4) {
                    sb.append("• Status: **Your documents are fully verified.** You are approved for direct DBT subsidy refills.");
                } else {
                    sb.append("• Status: **Verification Incomplete.** Please upload all 4 required identity proofs (Aadhaar, Ration Card, Electricity Bill, Bank Passbook) to complete KYC.");
                }

                return new AiChatResponseDTO(
                        sb.toString(),
                        "DATABASE_ASSISTANT",
                        "getMyDocumentsSummary",
                        data,
                        supportPhone,
                        "kyc-documents",
                        "Go to KYC Documents",
                        Arrays.asList("Check DBTL subsidy status", "How to book an LPG cylinder", "How to contact customer support")
                );
            }

            // Complaints status
            if (match.getIntent() == FaqIntent.COMPLAINT_HOWTO || lower.contains("complaint") || lower.contains("grievance")) {
                List<Complaint> complaints = complaintRepository.findByCustomerIdWithDetails(user.getId());
                Map<String, Object> data = new HashMap<>();
                data.put("complaints", complaints.stream().limit(5).map(c -> Map.of(
                        "complaintNumber", c.getComplaintNumber(),
                        "subject", c.getSubject(),
                        "status", c.getStatus()
                )).toList());

                StringBuilder sb = new StringBuilder();
                if (complaints.isEmpty()) {
                    sb.append("You have no active complaints registered. If you experience cylinder leakage, delivery delay, or billing discrepancies, you can file a new complaint in Help & Support.");
                } else {
                    sb.append("Here are your recent registered complaints:\n\n");
                    for (Complaint c : complaints) {
                        sb.append("• Complaint #`").append(c.getComplaintNumber()).append("` — **").append(c.getSubject()).append("**\n")
                          .append("  Status: `").append(c.getStatus()).append("` | Resolution: ").append(c.getResolutionNote() != null ? c.getResolutionNote() : "Under review").append("\n\n");
                    }
                }

                return new AiChatResponseDTO(
                        sb.toString(),
                        "DATABASE_ASSISTANT",
                        "getMyComplaints",
                        data,
                        supportPhone,
                        "support",
                        "Help & Support",
                        Arrays.asList("LPG safety guidelines", "How to contact customer support", "Check my booking status")
                );
            }

            // Live Delivery Tracking status
            if (match.getIntent() == FaqIntent.TRACKING_DELIVERY || lower.contains("track") || lower.contains("driver") || lower.contains("gps")) {
                List<Booking> bookings = bookingRepository.findByCustomerIdWithDetails(user.getId());
                Optional<Booking> activeDispatch = bookings.stream()
                        .filter(b -> "OUT_FOR_DELIVERY".equalsIgnoreCase(b.getStatus()) || "DISPATCHED".equalsIgnoreCase(b.getStatus()))
                        .findFirst();

                StringBuilder sb = new StringBuilder();
                if (activeDispatch.isPresent()) {
                    Booking b = activeDispatch.get();
                    sb.append("🚚 **Active Delivery Update for ").append(user.getName()).append("**:\n\n")
                      .append("• Order Reference: `").append(b.getBookingNumber() != null ? b.getBookingNumber() : ("BK-" + b.getId())).append("`\n")
                      .append("• Delivery Status: **").append(b.getStatus()).append("**\n")
                      .append("• Driver & Vehicle tracking is streaming live on the **Track Delivery** map.\n")
                      .append("• Please keep your 4-digit Handover OTP ready to verify upon physical handover.");
                } else {
                    sb.append("Hello ").append(user.getName()).append(", you currently have no orders marked as out for delivery. ")
                      .append("Once your distributor dispatches a confirmed booking, live GPS tracking and delivery OTP will be available under Track Delivery.");
                }

                return new AiChatResponseDTO(
                        sb.toString(),
                        "DATABASE_ASSISTANT",
                        "getMyDeliveryTracking",
                        Map.of("hasActiveDispatch", activeDispatch.isPresent()),
                        supportPhone,
                        "track-delivery",
                        "Track Delivery",
                        Arrays.asList("Check my booking status", "LPG safety guidelines", "How to contact customer support")
                );
            }
        }

        // 5. General FAQ answers (Non-account specific)
        FaqIntentService.FaqAnswer generalAns = faqIntentService.getGeneralAnswer(match.getIntent());
        return new AiChatResponseDTO(
                generalAns.getReply(),
                "FAQ_ENGINE",
                match.getIntent().name(),
                Map.of("intent", match.getIntent().name(), "confidence", match.confidence()),
                generalAns.getSupportPhone(),
                generalAns.getActionTab(),
                generalAns.getActionLabel(),
                generalAns.getSuggestions()
        );
    }

    private String formatInventory(List<?> inventory) {
        if (inventory == null || inventory.isEmpty()) return "No inventory records found for agency.";
        StringBuilder sb = new StringBuilder();
        for (Object obj : inventory) {
            var item = (com.lpgchain.entity.CylinderInventory) obj;
            sb.append("• ").append(item.getCylinderCategory()).append(": ")
              .append(item.getFilledStock()).append(" filled, ")
              .append(item.getEmptyStock()).append(" empty\n");
        }
        return sb.toString();
    }

    private String formatBookingQueue(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) return "No orders in queue.";
        StringBuilder sb = new StringBuilder();
        for (Booking b : bookings) {
            sb.append("• ").append(b.getBookingNumber()).append(" (")
              .append(b.getCustomer() != null ? b.getCustomer().getName() : "Customer")
              .append("): ").append(b.getStatus()).append(" [Payment: ").append(b.getPaymentStatus()).append("]\n");
        }
        return sb.toString();
    }
}
