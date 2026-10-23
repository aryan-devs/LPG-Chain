package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for recording an auditable allocation adjustment, return, or correction")
public class AllocationAdjustmentRequestDTO {

    @NotBlank(message = "adjustmentType is required")
    @Schema(description = "Adjustment classification (CORRECTION, RETURN, DAMAGE_WRITE_OFF, REALLOCATION)", example = "CORRECTION", requiredMode = Schema.RequiredMode.REQUIRED)
    private String adjustmentType;

    @NotNull(message = "adjustedQuantity is required")
    @Schema(description = "Adjusted cylinder quantity (positive to add, negative to deduct)", example = "-25", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer adjustedQuantity;

    @NotBlank(message = "reason is required")
    @Schema(description = "Audit reason or justification for this adjustment", example = "Depot stock discrepancy reconciliation approved by OMC Area Manager", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    @Schema(description = "External audit or sanction reference number", example = "OMC-AUDIT-2026-99")
    private String referenceNumber;

    public AllocationAdjustmentRequestDTO() {
    }

    public AllocationAdjustmentRequestDTO(String adjustmentType, Integer adjustedQuantity, String reason, String referenceNumber) {
        this.adjustmentType = adjustmentType;
        this.adjustedQuantity = adjustedQuantity;
        this.reason = reason;
        this.referenceNumber = referenceNumber;
    }

    public String getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(String adjustmentType) { this.adjustmentType = adjustmentType; }

    public Integer getAdjustedQuantity() { return adjustedQuantity; }
    public void setAdjustedQuantity(Integer adjustedQuantity) { this.adjustedQuantity = adjustedQuantity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
}
