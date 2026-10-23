package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Document review payload for approving or rejecting a KYC submission")
public class DocumentReviewDTO {

    @NotBlank(message = "Status is required (APPROVED or REJECTED)")
    private String status;

    private String rejectionReason;

    public DocumentReviewDTO() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
