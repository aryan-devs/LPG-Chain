package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Auditable allocation adjustment entry response")
public class AllocationAdjustmentResponseDTO {

    @Schema(description = "Adjustment record identifier", example = "501")
    private Long id;

    @Schema(description = "Target cylinder allocation identifier", example = "101")
    private Long allocationId;

    @Schema(description = "Adjustment type code", example = "CORRECTION")
    private String adjustmentType;

    @Schema(description = "Adjusted cylinder quantity (+/-)", example = "-25")
    private Integer adjustedQuantity;

    @Schema(description = "Audit reason or justification", example = "Depot stock discrepancy reconciliation")
    private String reason;

    @Schema(description = "Audit reference number", example = "OMC-AUDIT-2026-99")
    private String referenceNumber;

    @Schema(description = "Authorized admin user email who recorded this adjustment", example = "admin@lpgchain.demo")
    private String adjustedBy;

    @Schema(description = "Timestamp when adjustment was recorded", example = "2026-10-05T14:30:00")
    private LocalDateTime adjustedAt;

    public AllocationAdjustmentResponseDTO() {
    }

    public AllocationAdjustmentResponseDTO(Long id, Long allocationId, String adjustmentType,
                                          Integer adjustedQuantity, String reason, String referenceNumber,
                                          String adjustedBy, LocalDateTime adjustedAt) {
        this.id = id;
        this.allocationId = allocationId;
        this.adjustmentType = adjustmentType;
        this.adjustedQuantity = adjustedQuantity;
        this.reason = reason;
        this.referenceNumber = referenceNumber;
        this.adjustedBy = adjustedBy;
        this.adjustedAt = adjustedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAllocationId() { return allocationId; }
    public void setAllocationId(Long allocationId) { this.allocationId = allocationId; }

    public String getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(String adjustmentType) { this.adjustmentType = adjustmentType; }

    public Integer getAdjustedQuantity() { return adjustedQuantity; }
    public void setAdjustedQuantity(Integer adjustedQuantity) { this.adjustedQuantity = adjustedQuantity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getAdjustedBy() { return adjustedBy; }
    public void setAdjustedBy(String adjustedBy) { this.adjustedBy = adjustedBy; }

    public LocalDateTime getAdjustedAt() { return adjustedAt; }
    public void setAdjustedAt(LocalDateTime adjustedAt) { this.adjustedAt = adjustedAt; }
}
