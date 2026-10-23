package com.lpgchain.service;

import com.lpgchain.dto.BillingInvoiceDTO;
import com.lpgchain.dto.ConfirmCodRequestDTO;
import com.lpgchain.dto.PaymentResponseDTO;
import com.lpgchain.dto.SimulatePaymentRequestDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.Payment;
import com.lpgchain.entity.User;
import com.lpgchain.exception.ResourceNotFoundException;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.PaymentRepository;
import com.lpgchain.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final DistributorMembershipRepository distributorMembershipRepository;
    private final EntityMapper entityMapper;
    private final RealtimeEventPublisher realtimeEventPublisher;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          UserRepository userRepository,
                          DistributorMembershipRepository distributorMembershipRepository,
                          EntityMapper entityMapper,
                          RealtimeEventPublisher realtimeEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.distributorMembershipRepository = distributorMembershipRepository;
        this.entityMapper = entityMapper;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Unauthorized: Authentication required");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    private boolean hasRole(String roleName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_" + roleName) || a.getAuthority().equalsIgnoreCase(roleName));
    }

    /**
     * Creates or retrieves a demo QR payment session for an active booking.
     */
    public PaymentResponseDTO getOrCreateDemoQrForBooking(Long bookingId) {
        User currentUser = getAuthenticatedUser();
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        // ReBAC check
        if (!hasRole("ADMIN") && !booking.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Forbidden: Cannot access payments for other customers' bookings");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setBooking(booking);
                    p.setCustomer(booking.getCustomer());
                    p.setAmount(booking.getTotalAmount() != null ? booking.getTotalAmount() : 853.0);
                    p.setSubsidyAmount(booking.getSubsidyAmount() != null ? booking.getSubsidyAmount() : 200.0);
                    p.setNetAmount(booking.getNetAmount() != null ? booking.getNetAmount() : 653.0);
                    p.setPaymentMethod("DEMO_QR");
                    p.setPaymentStatus("PENDING");
                    p.setTransactionReference("TXN-DEMO-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
                    p.setIsSimulated(true);
                    return paymentRepository.save(p);
                });

        PaymentResponseDTO dto = entityMapper.toPaymentResponseDTO(payment);
        dto.setQrCodePayload(generateQrPayload(payment, booking));
        return dto;
    }

    private Payment resolvePaymentByReference(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new ResourceNotFoundException("Transaction reference or booking reference is required");
        }
        String ref = reference.trim();
        Optional<Payment> pOpt = paymentRepository.findByTransactionReference(ref);
        if (pOpt.isPresent()) return pOpt.get();

        // Try as numeric booking ID
        try {
            String digitsOnly = ref.replaceAll("[^0-9]", "");
            if (!digitsOnly.isEmpty()) {
                Long bId = Long.parseLong(digitsOnly);
                Optional<Payment> byBooking = paymentRepository.findByBookingId(bId);
                if (byBooking.isPresent()) return byBooking.get();
            }
        } catch (Exception ignored) {}

        // Try as booking number (e.g. BK-...)
        Optional<Booking> bOpt = bookingRepository.findByBookingNumber(ref);
        if (bOpt.isPresent()) {
            return paymentRepository.findByBookingId(bOpt.get().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for booking: " + ref));
        }

        throw new ResourceNotFoundException("Payment record not found for reference: " + ref);
    }

    /**
     * Simulates successful payment for a demo QR reference.
     * Enforces idempotency, customer ownership check, and broadcasts updates.
     */
    public PaymentResponseDTO simulateSuccessfulPayment(SimulatePaymentRequestDTO request) {
        User currentUser = getAuthenticatedUser();
        Payment payment = resolvePaymentByReference(request.getTransactionReference());

        if (!hasRole("ADMIN") && !payment.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Forbidden: You cannot confirm payment on another customer's booking");
        }

        // Idempotency: If already paid, return without duplicating
        if ("PAID".equals(payment.getPaymentStatus())) {
            PaymentResponseDTO dto = entityMapper.toPaymentResponseDTO(payment);
            dto.setQrCodePayload(generateQrPayload(payment, payment.getBooking()));
            return dto;
        }

        payment.setPaymentStatus("PAID");
        payment.setIsSimulated(true);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        if (booking != null) {
            booking.setPaymentStatus("PAID");
            booking.setPaymentReference(payment.getTransactionReference());
            bookingRepository.save(booking);
        }

        PaymentResponseDTO dto = entityMapper.toPaymentResponseDTO(payment);
        dto.setQrCodePayload(generateQrPayload(payment, booking));

        // Real-time notification across all authorized views
        realtimeEventPublisher.publishCustomerEvent(payment.getCustomer().getId(), "PAYMENT_CONFIRMED", dto);
        if (booking != null && booking.getDistributor() != null) {
            realtimeEventPublisher.publishDistributorEvent(booking.getDistributor().getId(), "PAYMENT_CONFIRMED", dto);
        }
        realtimeEventPublisher.publishAdminEvent("PAYMENT_CONFIRMED", dto);

        return dto;
    }

    /**
     * Confirms cash collection for Cash on Delivery.
     * STRICT SAFEGUARD: Only authorized Distributor or Admin can confirm cash collection.
     * Customers are strictly prohibited from confirming their own COD.
     */
    public PaymentResponseDTO confirmCodCollection(ConfirmCodRequestDTO request) {
        User currentUser = getAuthenticatedUser();

        // Prevent customer self-confirmation
        if (!hasRole("ADMIN") && !hasRole("DISTRIBUTOR")) {
            throw new AccessDeniedException("Forbidden: Customers cannot confirm their own Cash on Delivery payment");
        }

        Booking booking = bookingRepository.findByIdWithDetails(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + request.getBookingId()));

        // If distributor, verify membership with booking distributor
        if (!hasRole("ADMIN") && booking.getDistributor() != null) {
            boolean hasMembership = distributorMembershipRepository.existsByUserIdAndDistributorIdAndStatusIgnoreCase(
                    currentUser.getId(), booking.getDistributor().getId(), "ACTIVE");
            if (!hasMembership) {
                throw new AccessDeniedException("Forbidden: Not an authorized distributor for this booking");
            }
        }

        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setBooking(booking);
                    p.setCustomer(booking.getCustomer());
                    p.setAmount(booking.getTotalAmount() != null ? booking.getTotalAmount() : 853.0);
                    p.setSubsidyAmount(booking.getSubsidyAmount() != null ? booking.getSubsidyAmount() : 200.0);
                    p.setNetAmount(booking.getNetAmount() != null ? booking.getNetAmount() : 653.0);
                    p.setPaymentMethod("CASH_ON_DELIVERY");
                    p.setPaymentStatus("PENDING");
                    p.setTransactionReference("COD-" + booking.getId() + "-" + System.currentTimeMillis());
                    p.setIsSimulated(false);
                    return p;
                });

        payment.setPaymentStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        payment.setCollectedBy(currentUser.getName() + " (" + currentUser.getEmail() + ")");
        paymentRepository.save(payment);

        booking.setPaymentStatus("PAID");
        booking.setPaymentReference(payment.getTransactionReference());
        bookingRepository.save(booking);

        PaymentResponseDTO dto = entityMapper.toPaymentResponseDTO(payment);

        // Real-time broadcast
        realtimeEventPublisher.publishCustomerEvent(payment.getCustomer().getId(), "PAYMENT_CONFIRMED", dto);
        if (booking.getDistributor() != null) {
            realtimeEventPublisher.publishDistributorEvent(booking.getDistributor().getId(), "PAYMENT_CONFIRMED", dto);
        }
        realtimeEventPublisher.publishAdminEvent("PAYMENT_CONFIRMED", dto);

        return dto;
    }

    /**
     * Generates immutable billing invoice from database records.
     */
    @Transactional(readOnly = true)
    public BillingInvoiceDTO getInvoiceForBooking(Long bookingId) {
        User currentUser = getAuthenticatedUser();
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        // ReBAC authorization check
        if (!hasRole("ADMIN") && !booking.getCustomer().getId().equals(currentUser.getId())) {
            if (hasRole("DISTRIBUTOR")) {
                if (booking.getDistributor() == null || !distributorMembershipRepository.existsByUserIdAndDistributorIdAndStatusIgnoreCase(
                        currentUser.getId(), booking.getDistributor().getId(), "ACTIVE")) {
                    throw new AccessDeniedException("Forbidden: Not authorized to view invoice for this booking");
                }
            } else {
                throw new AccessDeniedException("Forbidden: You cannot view invoices belonging to other users");
            }
        }

        Optional<Payment> paymentOpt = paymentRepository.findByBookingId(bookingId);

        BillingInvoiceDTO invoice = new BillingInvoiceDTO();
        invoice.setInvoiceNumber("INV-" + booking.getId() + "-" + (booking.getBookingNumber() != null ? booking.getBookingNumber() : ""));
        invoice.setBookingId(booking.getId());
        invoice.setBookingNumber(booking.getBookingNumber());
        invoice.setBookingDate(booking.getBookingDate());
        invoice.setInvoiceDate(paymentOpt.map(Payment::getPaidAt).orElse(booking.getBookingDate()));

        User cust = booking.getCustomer();
        if (cust != null) {
            invoice.setCustomerId(cust.getId());
            invoice.setCustomerName(cust.getName());
            invoice.setCustomerEmail(cust.getEmail());
            invoice.setCustomerPhone(cust.getPhone());
            invoice.setConsumerNumber(cust.getConsumerNumber());
        }
        invoice.setDeliveryAddress(booking.getDeliveryAddress());

        Distributor dist = booking.getDistributor();
        if (dist != null) {
            invoice.setDistributorId(dist.getId());
            invoice.setDistributorName(dist.getAgencyName());
        }

        invoice.setCylinderCategory(booking.getCylinderCategory() != null ? booking.getCylinderCategory() : "DOMESTIC_14_2");
        invoice.setCapacityKg(booking.getCapacityKg() != null ? booking.getCapacityKg() : 14.2);
        invoice.setQuantity(booking.getQuantity());
        invoice.setUnitPrice(booking.getUnitPrice() != null ? booking.getUnitPrice() : 853.0);
        invoice.setTotalAmount(booking.getTotalAmount() != null ? booking.getTotalAmount() : 853.0);
        invoice.setSubsidyAmount(booking.getSubsidyAmount() != null ? booking.getSubsidyAmount() : 200.0);
        invoice.setNetAmount(booking.getNetAmount() != null ? booking.getNetAmount() : 653.0);

        if (paymentOpt.isPresent()) {
            Payment p = paymentOpt.get();
            invoice.setPaymentMethod(p.getPaymentMethod());
            invoice.setPaymentStatus(p.getPaymentStatus());
            invoice.setTransactionReference(p.getTransactionReference());
            invoice.setIsSimulated(p.getIsSimulated());
            invoice.setPaidAt(p.getPaidAt());
        } else {
            invoice.setPaymentMethod(booking.getPaymentMethod() != null ? booking.getPaymentMethod() : "DEMO_QR");
            invoice.setPaymentStatus(booking.getPaymentStatus() != null ? booking.getPaymentStatus() : "PENDING");
            invoice.setTransactionReference(booking.getPaymentReference());
        }

        return invoice;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getMyPayments() {
        User currentUser = getAuthenticatedUser();
        return paymentRepository.findByCustomerIdWithDetails(currentUser.getId()).stream()
                .map(p -> {
                    PaymentResponseDTO dto = entityMapper.toPaymentResponseDTO(p);
                    dto.setQrCodePayload(generateQrPayload(p, p.getBooking()));
                    return dto;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getDistributorPayments(Long distributorId) {
        User currentUser = getAuthenticatedUser();
        if (!hasRole("ADMIN")) {
            boolean hasMembership = distributorMembershipRepository.existsByUserIdAndDistributorIdAndStatusIgnoreCase(
                    currentUser.getId(), distributorId, "ACTIVE");
            if (!hasMembership) {
                throw new AccessDeniedException("Forbidden: Not authorized to view payments for distributor agency: " + distributorId);
            }
        }

        return paymentRepository.findByDistributorIdWithDetails(distributorId).stream()
                .map(entityMapper::toPaymentResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getAllPayments() {
        if (!hasRole("ADMIN")) {
            throw new AccessDeniedException("Forbidden: Administrator access required");
        }
        return paymentRepository.findAllWithDetails().stream()
                .map(entityMapper::toPaymentResponseDTO)
                .toList();
    }

    /**
     * Creates a Razorpay test order (if credentials configured) or returns a demo fallback order.
     */
    public com.lpgchain.dto.RazorpayOrderResponseDTO createRazorpayOrder(Long bookingId) {
        User currentUser = getAuthenticatedUser();
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!hasRole("ADMIN") && !booking.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Forbidden: Not authorized to initiate payment for this booking");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setBooking(booking);
                    p.setCustomer(booking.getCustomer());
                    p.setAmount(booking.getTotalAmount() != null ? booking.getTotalAmount() : 853.0);
                    p.setSubsidyAmount(booking.getSubsidyAmount() != null ? booking.getSubsidyAmount() : 200.0);
                    p.setNetAmount(booking.getNetAmount() != null ? booking.getNetAmount() : 653.0);
                    p.setPaymentMethod("ONLINE");
                    p.setPaymentStatus("PENDING");
                    p.setTransactionReference("TXN-PAY-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
                    p.setIsSimulated(true);
                    return paymentRepository.save(p);
                });

        double payable = payment.getNetAmount() != null ? payment.getNetAmount() : 653.0;
        long amountInPaise = Math.round(payable * 100);

        com.lpgchain.dto.RazorpayOrderResponseDTO res = new com.lpgchain.dto.RazorpayOrderResponseDTO();
        res.setBookingId(booking.getId());
        res.setBookingNumber(booking.getBookingNumber());
        res.setPaymentReference(payment.getTransactionReference());
        res.setAmount(payable);
        res.setAmountInPaise(amountInPaise);
        res.setCurrency("INR");
        res.setCustomerName(booking.getCustomer().getName());
        res.setCustomerEmail(booking.getCustomer().getEmail());
        res.setCustomerPhone(booking.getCustomer().getPhone());

        boolean configured = razorpayKeyId != null && !razorpayKeyId.isBlank() && !razorpayKeyId.contains("placeholder");
        res.setConfigured(configured);

        if (configured) {
            res.setKeyId(razorpayKeyId);
            res.setOrderId("order_test_" + booking.getId() + "_" + System.currentTimeMillis());
            res.setNotes("LPG-Chain Refill Booking #" + booking.getBookingNumber());
        } else {
            res.setKeyId("");
            res.setOrderId("order_demo_" + booking.getId() + "_" + System.currentTimeMillis());
            res.setNotes("Demo mode — No external merchant account required. Prototype settlement active.");
        }

        return res;
    }

    /**
     * Verifies Razorpay payment signature or test demo confirmation.
     */
    public PaymentResponseDTO verifyRazorpayPayment(com.lpgchain.dto.RazorpayVerifyRequestDTO request) {
        User currentUser = getAuthenticatedUser();
        Booking booking = bookingRepository.findByIdWithDetails(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + request.getBookingId()));

        if (!hasRole("ADMIN") && !booking.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Forbidden: Not authorized to verify payment for this booking");
        }

        Payment payment = paymentRepository.findByBookingId(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for booking: " + request.getBookingId()));

        boolean isRealConfigured = razorpayKeySecret != null && !razorpayKeySecret.isBlank() && !razorpayKeySecret.contains("placeholder");
        if (isRealConfigured && request.getRazorpaySignature() != null && !request.getRazorpaySignature().isBlank()) {
            boolean validSig = verifySignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature(), razorpayKeySecret);
            if (!validSig) {
                throw new IllegalArgumentException("Razorpay payment signature verification failed");
            }
        }

        payment.setPaymentStatus("PAID");
        payment.setIsSimulated(!isRealConfigured);
        payment.setPaidAt(LocalDateTime.now());
        payment.setPaymentMethod("ONLINE");
        paymentRepository.save(payment);

        booking.setPaymentStatus("PAID");
        booking.setPaymentReference(payment.getTransactionReference());
        bookingRepository.save(booking);

        PaymentResponseDTO dto = entityMapper.toPaymentResponseDTO(payment);
        dto.setQrCodePayload(generateQrPayload(payment, booking));

        realtimeEventPublisher.publishCustomerEvent(payment.getCustomer().getId(), "PAYMENT_CONFIRMED", dto);
        if (booking.getDistributor() != null) {
            realtimeEventPublisher.publishDistributorEvent(booking.getDistributor().getId(), "PAYMENT_CONFIRMED", dto);
        }
        realtimeEventPublisher.publishAdminEvent("PAYMENT_CONFIRMED", dto);

        return dto;
    }

    /**
     * Simulates payment failure or user cancellation for testing failure recovery.
     */
    public PaymentResponseDTO simulateFailedPayment(SimulatePaymentRequestDTO request) {
        User currentUser = getAuthenticatedUser();
        Payment payment = resolvePaymentByReference(request.getTransactionReference());

        if (!hasRole("ADMIN") && !payment.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Forbidden: Cannot modify payment belonging to another customer");
        }

        if ("PAID".equals(payment.getPaymentStatus())) {
            throw new IllegalStateException("Cannot fail a payment that is already PAID");
        }

        payment.setPaymentStatus("FAILED");
        paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        if (booking != null) {
            booking.setPaymentStatus("FAILED");
            bookingRepository.save(booking);
        }

        PaymentResponseDTO dto = entityMapper.toPaymentResponseDTO(payment);
        dto.setQrCodePayload(generateQrPayload(payment, booking));

        realtimeEventPublisher.publishCustomerEvent(payment.getCustomer().getId(), "PAYMENT_FAILED", dto);
        realtimeEventPublisher.publishAdminEvent("PAYMENT_FAILED", dto);

        return dto;
    }

    /**
     * Resets a failed or cancelled payment session allowing the customer to retry without creating duplicate bookings.
     */
    public PaymentResponseDTO retryPayment(Long bookingId) {
        User currentUser = getAuthenticatedUser();
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!hasRole("ADMIN") && !booking.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Forbidden: Cannot retry payment for another customer's booking");
        }

        if ("PAID".equals(booking.getPaymentStatus())) {
            throw new IllegalStateException("Booking is already PAID. Retry is not permitted.");
        }

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setBooking(booking);
                    p.setCustomer(booking.getCustomer());
                    p.setAmount(booking.getTotalAmount() != null ? booking.getTotalAmount() : 853.0);
                    p.setSubsidyAmount(booking.getSubsidyAmount() != null ? booking.getSubsidyAmount() : 200.0);
                    p.setNetAmount(booking.getNetAmount() != null ? booking.getNetAmount() : 653.0);
                    p.setPaymentMethod("DEMO_QR");
                    p.setIsSimulated(true);
                    return p;
                });

        payment.setPaymentStatus("PENDING");
        payment.setTransactionReference("TXN-RETRY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setIsSimulated(true);
        paymentRepository.save(payment);

        booking.setPaymentStatus("PENDING");
        booking.setPaymentReference(payment.getTransactionReference());
        bookingRepository.save(booking);

        PaymentResponseDTO dto = entityMapper.toPaymentResponseDTO(payment);
        dto.setQrCodePayload(generateQrPayload(payment, booking));

        realtimeEventPublisher.publishCustomerEvent(booking.getCustomer().getId(), "PAYMENT_RETRY_INITIATED", dto);
        return dto;
    }

    private boolean verifySignature(String orderId, String paymentId, String signature, String secret) {
        if (secret == null || secret.isBlank() || signature == null || signature.isBlank()) {
            return false;
        }
        try {
            String data = orderId + "|" + paymentId;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equalsIgnoreCase(signature.trim());
        } catch (Exception e) {
            return false;
        }
    }

    private String generateQrPayload(Payment payment, Booking booking) {
        String bookingNum = booking != null && booking.getBookingNumber() != null ? booking.getBookingNumber() : "LPG-ORDER";
        double amount = payment.getNetAmount() != null ? payment.getNetAmount() : 653.0;
        return String.format("upi://pay?pa=demo-lpgchain@upi&pn=LPG+Chain+Demo&am=%.2f&cu=INR&tr=%s&tn=Refill+%s",
                amount, payment.getTransactionReference(), bookingNum);
    }
}

