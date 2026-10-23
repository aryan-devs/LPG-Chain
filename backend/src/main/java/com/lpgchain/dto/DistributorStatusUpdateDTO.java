package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DistributorStatusUpdateDTO — Payload for administrative status update on an LPG distributor agency.
 */
@Schema(description = "Request payload for updating distributor agency status")
public class DistributorStatusUpdateDTO {

    @NotBlank(message = "Status cannot be blank")
    @Pattern(regexp = "^(ACTIVE|SUSPENDED|PENDING_VERIFICATION)$",
             message = "Status must be one of: ACTIVE, SUSPENDED, PENDING_VERIFICATION")
    @Schema(description = "New agency status", example = "ACTIVE", allowableValues = {"ACTIVE", "SUSPENDED", "PENDING_VERIFICATION"})
    private String status;

    public DistributorStatusUpdateDTO() {
    }

    public DistributorStatusUpdateDTO(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
