package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.dto.BillingInvoiceDTO;
import com.lpgchain.dto.ConfirmCodRequestDTO;
import com.lpgchain.dto.PaymentResponseDTO;
import com.lpgchain.dto.SimulatePaymentRequestDTO;
import com.lpgchain.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments & Invoices", description = "Endpoints for Demo QR payments, Cash on Delivery confirmation, and Invoices")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/qr/{bookingId}")
    @Operation(summary = "Get or create demo UPI QR payment payload for a booking")
    public ResponseEntity<PaymentResponseDTO> getDemoQr(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getOrCreateDemoQrForBooking(bookingId));
    }

    @PostMapping("/simulate-success")
    @Operation(summary = "Simulate successful payment for a demo QR transaction reference")
    public ResponseEntity<PaymentResponseDTO> simulateSuccess(@Valid @RequestBody SimulatePaymentRequestDTO request) {
        return ResponseEntity.ok(paymentService.simulateSuccessfulPayment(request));
    }

    @PostMapping("/simulate-failure")
    @Operation(summary = "Simulate payment failure or cancellation for demo transaction reference")
    public ResponseEntity<PaymentResponseDTO> simulateFailure(@Valid @RequestBody SimulatePaymentRequestDTO request) {
        return ResponseEntity.ok(paymentService.simulateFailedPayment(request));
    }

    @PostMapping("/retry/{bookingId}")
    @Operation(summary = "Retry payment on a pending or failed booking without creating duplicates")
    public ResponseEntity<PaymentResponseDTO> retryPayment(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.retryPayment(bookingId));
    }

    @PostMapping("/razorpay/order/{bookingId}")
    @Operation(summary = "Create Razorpay test order (if configured) or initialize demo checkout")
    public ResponseEntity<com.lpgchain.dto.RazorpayOrderResponseDTO> createRazorpayOrder(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(bookingId));
    }

    @PostMapping("/razorpay/verify")
    @Operation(summary = "Verify Razorpay payment signature or test demo confirmation")
    public ResponseEntity<PaymentResponseDTO> verifyRazorpayPayment(@Valid @RequestBody com.lpgchain.dto.RazorpayVerifyRequestDTO request) {
        return ResponseEntity.ok(paymentService.verifyRazorpayPayment(request));
    }

    @PostMapping("/confirm-cod")
    @Operation(summary = "Confirm cash collection for Cash on Delivery (Distributor / Admin only)")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN')")
    public ResponseEntity<PaymentResponseDTO> confirmCod(@Valid @RequestBody ConfirmCodRequestDTO request) {
        return ResponseEntity.ok(paymentService.confirmCodCollection(request));
    }

    @GetMapping("/invoice/{bookingId}")
    @Operation(summary = "Get immutable billing invoice generated from persisted booking & payment")
    public ResponseEntity<BillingInvoiceDTO> getInvoice(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getInvoiceForBooking(bookingId));
    }

    @GetMapping("/my")
    @Operation(summary = "Get payment history for current customer")
    public ResponseEntity<List<PaymentResponseDTO>> getMyPayments() {
        return ResponseEntity.ok(paymentService.getMyPayments());
    }

    @GetMapping("/distributor/{distributorId}")
    @Operation(summary = "Get payment records for assigned distributor agency")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getDistributorPayments(@PathVariable Long distributorId) {
        return ResponseEntity.ok(paymentService.getDistributorPayments(distributorId));
    }

    @GetMapping("/all")
    @Operation(summary = "Get platform-wide payment records (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }
}
