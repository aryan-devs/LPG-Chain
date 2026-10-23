package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Detailed cylinder allocation record response")
public class CylinderAllocationResponseDTO {

    @Schema(description = "Allocation unique identifier", example = "101")
    private Long id;

    @Schema(description = "Distributor agency identifier", example = "1")
    private Long distributorId;

    @Schema(description = "Distributor agency trade name", example = "City Gas Agency")
    private String distributorName;

    @Schema(description = "Cylinder category code", example = "DOMESTIC_14_2")
    private String cylinderCategory;

    @Schema(description = "Cylinder category display name", example = "Domestic 14.2 kg")
    private String categoryDisplayName;

    @Schema(description = "Nominal cylinder capacity in kg", example = "14.2")
    private Double capacityKg;

    @Schema(description = "Officially allocated cylinder quantity", example = "5000")
    private Integer allocatedQuantity;

    @Schema(description = "Net cumulative adjustment quantity (+/-)", example = "-50")
    private Integer adjustmentQuantity;

    @Schema(description = "Net adjusted allocation quantity", example = "4950")
    private Integer netAllocatedQuantity;

    @Schema(description = "Reporting calendar year", example = "2026")
    private Integer reportingYear;

    @Schema(description = "Reporting calendar month (1-12)", example = "10")
    private Integer reportingMonth;

    @Schema(description = "Reporting period code (YYYY-MM)", example = "2026-10")
    private String reportingPeriod;

    @Schema(description = "Allocation effective date", example = "2026-10-01")
    private LocalDate allocationDate;

    @Schema(description = "Official OMC source reference ID", example = "MOPNG-ALLOC-2026-OCT-01")
    private String sourceReference;

    @Schema(description = "Record creation timestamp", example = "2026-10-01T08:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp", example = "2026-10-05T14:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Admin user who created this allocation", example = "admin@lpgchain.demo")
    private String createdBy;

    @Schema(description = "Admin user who last updated this allocation", example = "admin@lpgchain.demo")
    private String updatedBy;

    public CylinderAllocationResponseDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDistributorId() { return distributorId; }
    public void setDistributorId(Long distributorId) { this.distributorId = distributorId; }

    public String getDistributorName() { return distributorName; }
    public void setDistributorName(String distributorName) { this.distributorName = distributorName; }

    public String getCylinderCategory() { return cylinderCategory; }
    public void setCylinderCategory(String cylinderCategory) { this.cylinderCategory = cylinderCategory; }

    public String getCategoryDisplayName() { return categoryDisplayName; }
    public void setCategoryDisplayName(String categoryDisplayName) { this.categoryDisplayName = categoryDisplayName; }

    public Double getCapacityKg() { return capacityKg; }
    public void setCapacityKg(Double capacityKg) { this.capacityKg = capacityKg; }

    public Integer getAllocatedQuantity() { return allocatedQuantity; }
    public void setAllocatedQuantity(Integer allocatedQuantity) { this.allocatedQuantity = allocatedQuantity; }

    public Integer getAdjustmentQuantity() { return adjustmentQuantity; }
    public void setAdjustmentQuantity(Integer adjustmentQuantity) { this.adjustmentQuantity = adjustmentQuantity; }

    public Integer getNetAllocatedQuantity() { return netAllocatedQuantity; }
    public void setNetAllocatedQuantity(Integer netAllocatedQuantity) { this.netAllocatedQuantity = netAllocatedQuantity; }

    public Integer getReportingYear() { return reportingYear; }
    public void setReportingYear(Integer reportingYear) { this.reportingYear = reportingYear; }

    public Integer getReportingMonth() { return reportingMonth; }
    public void setReportingMonth(Integer reportingMonth) { this.reportingMonth = reportingMonth; }

    public String getReportingPeriod() { return reportingPeriod; }
    public void setReportingPeriod(String reportingPeriod) { this.reportingPeriod = reportingPeriod; }

    public LocalDate getAllocationDate() { return allocationDate; }
    public void setAllocationDate(LocalDate allocationDate) { this.allocationDate = allocationDate; }

    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
