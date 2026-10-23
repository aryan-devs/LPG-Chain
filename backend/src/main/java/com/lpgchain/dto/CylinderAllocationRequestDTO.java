package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Request payload for creating or registering an official distributor cylinder quota allocation")
public class CylinderAllocationRequestDTO {

    @NotNull(message = "distributorId is required")
    @Schema(description = "Distributor agency identifier", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long distributorId;

    @NotBlank(message = "cylinderCategory is required")
    @Schema(description = "Cylinder category code (DOMESTIC_14_2, DOMESTIC_5_0, COMMERCIAL_19_0)", example = "DOMESTIC_14_2", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cylinderCategory;

    @Schema(description = "Nominal cylinder capacity in kg (optional, derived from category if omitted)", example = "14.2")
    private Double capacityKg;

    @NotNull(message = "allocatedQuantity is required")
    @Min(value = 0, message = "allocatedQuantity cannot be negative")
    @Schema(description = "Officially allocated cylinder quantity", example = "5000", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer allocatedQuantity;

    @NotNull(message = "reportingYear is required")
    @Min(value = 2020, message = "reportingYear must be 2020 or later")
    @Schema(description = "Reporting calendar year", example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer reportingYear;

    @NotNull(message = "reportingMonth is required")
    @Min(value = 1, message = "reportingMonth must be between 1 and 12")
    @Max(value = 12, message = "reportingMonth must be between 1 and 12")
    @Schema(description = "Reporting calendar month (1-12)", example = "10", minimum = "1", maximum = "12", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer reportingMonth;

    @Schema(description = "Official quota allocation date (defaults to current date if omitted)", example = "2026-10-01")
    private LocalDate allocationDate;

    @Schema(description = "Official OMC source reference, tender or depot allotment order ID", example = "MOPNG-ALLOC-2026-OCT-01")
    private String sourceReference;

    public CylinderAllocationRequestDTO() {
    }

    public CylinderAllocationRequestDTO(Long distributorId, String cylinderCategory, Double capacityKg,
                                       Integer allocatedQuantity, Integer reportingYear, Integer reportingMonth,
                                       LocalDate allocationDate, String sourceReference) {
        this.distributorId = distributorId;
        this.cylinderCategory = cylinderCategory;
        this.capacityKg = capacityKg;
        this.allocatedQuantity = allocatedQuantity;
        this.reportingYear = reportingYear;
        this.reportingMonth = reportingMonth;
        this.allocationDate = allocationDate;
        this.sourceReference = sourceReference;
    }

    public Long getDistributorId() { return distributorId; }
    public void setDistributorId(Long distributorId) { this.distributorId = distributorId; }

    public String getCylinderCategory() { return cylinderCategory; }
    public void setCylinderCategory(String cylinderCategory) { this.cylinderCategory = cylinderCategory; }

    public Double getCapacityKg() { return capacityKg; }
    public void setCapacityKg(Double capacityKg) { this.capacityKg = capacityKg; }

    public Integer getAllocatedQuantity() { return allocatedQuantity; }
    public void setAllocatedQuantity(Integer allocatedQuantity) { this.allocatedQuantity = allocatedQuantity; }

    public Integer getReportingYear() { return reportingYear; }
    public void setReportingYear(Integer reportingYear) { this.reportingYear = reportingYear; }

    public Integer getReportingMonth() { return reportingMonth; }
    public void setReportingMonth(Integer reportingMonth) { this.reportingMonth = reportingMonth; }

    public LocalDate getAllocationDate() { return allocationDate; }
    public void setAllocationDate(LocalDate allocationDate) { this.allocationDate = allocationDate; }

    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }
}
