package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Category-wise cylinder allocation and sales transparency breakdown")
public class CategoryTransparencyDTO {

    @Schema(description = "Cylinder category code", example = "DOMESTIC_14_2")
    private String cylinderCategory;

    @Schema(description = "Cylinder category display name", example = "Domestic 14.2 kg")
    private String categoryDisplayName;

    @Schema(description = "Nominal cylinder capacity in kg", example = "14.2")
    private Double capacityKg;

    @Schema(description = "Indicates whether an official allocation record exists for this period", example = "true")
    private boolean allocated;

    @Schema(description = "Officially allocated cylinder quota", example = "5000")
    private Integer allocatedQuantity;

    @Schema(description = "Net cumulative adjustments recorded (+/-)", example = "-50")
    private Integer adjustmentQuantity;

    @Schema(description = "Net adjusted allocation quantity", example = "4950")
    private Integer netAllocatedQuantity;

    @Schema(description = "Actual verified delivered cylinder quantity from completed sales", example = "3210")
    private Integer deliveredQuantity;

    @Schema(description = "Remaining allocation balance (net allocated minus completed deliveries)", example = "1740")
    private Integer remainingAllocation;

    @Schema(description = "Allocation utilization rate percentage", example = "64.85")
    private Double utilizationPercentage;

    @Schema(description = "Official OMC allocation reference code", example = "MOPNG-ALLOC-2026-OCT-01")
    private String sourceReference;

    @Schema(description = "Last updated timestamp for this category's allocation/deliveries", example = "2026-10-10T15:30:00")
    private LocalDateTime lastUpdated;

    @Schema(description = "Transparency verification status note", example = "Official allocation verified against OMC quota records")
    private String statusMessage;

    public CategoryTransparencyDTO() {
    }

    public String getCylinderCategory() { return cylinderCategory; }
    public void setCylinderCategory(String cylinderCategory) { this.cylinderCategory = cylinderCategory; }

    public String getCategoryDisplayName() { return categoryDisplayName; }
    public void setCategoryDisplayName(String categoryDisplayName) { this.categoryDisplayName = categoryDisplayName; }

    public Double getCapacityKg() { return capacityKg; }
    public void setCapacityKg(Double capacityKg) { this.capacityKg = capacityKg; }

    public boolean isAllocated() { return allocated; }
    public void setAllocated(boolean allocated) { this.allocated = allocated; }

    public Integer getAllocatedQuantity() { return allocatedQuantity; }
    public void setAllocatedQuantity(Integer allocatedQuantity) { this.allocatedQuantity = allocatedQuantity; }

    public Integer getAdjustmentQuantity() { return adjustmentQuantity; }
    public void setAdjustmentQuantity(Integer adjustmentQuantity) { this.adjustmentQuantity = adjustmentQuantity; }

    public Integer getNetAllocatedQuantity() { return netAllocatedQuantity; }
    public void setNetAllocatedQuantity(Integer netAllocatedQuantity) { this.netAllocatedQuantity = netAllocatedQuantity; }

    public Integer getDeliveredQuantity() { return deliveredQuantity; }
    public void setDeliveredQuantity(Integer deliveredQuantity) { this.deliveredQuantity = deliveredQuantity; }

    public Integer getRemainingAllocation() { return remainingAllocation; }
    public void setRemainingAllocation(Integer remainingAllocation) { this.remainingAllocation = remainingAllocation; }

    public Double getUtilizationPercentage() { return utilizationPercentage; }
    public void setUtilizationPercentage(Double utilizationPercentage) { this.utilizationPercentage = utilizationPercentage; }

    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
}
