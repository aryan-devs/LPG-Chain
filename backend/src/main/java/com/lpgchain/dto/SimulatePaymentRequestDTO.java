package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to simulate successful payment for a demo QR reference")
public class SimulatePaymentRequestDTO {

    @NotBlank(message = "transactionReference is required")
    private String transactionReference;

    private String reason;

    public SimulatePaymentRequestDTO() {}

    public SimulatePaymentRequestDTO(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public SimulatePaymentRequestDTO(String transactionReference, String reason) {
        this.transactionReference = transactionReference;
        this.reason = reason;
    }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
