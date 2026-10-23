package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Distributor cylinder allocation and sales transparency report for a reporting period")
public class TransparencyReportDTO {

    @Schema(description = "Distributor agency identifier", example = "1")
    private Long distributorId;

    @Schema(description = "Distributor agency trade name", example = "City Gas Agency")
    private String distributorName;

    @Schema(description = "Distributor city and state", example = "Mumbai, Maharashtra")
    private String location;

    @Schema(description = "Reporting calendar year", example = "2026")
    private Integer reportingYear;

    @Schema(description = "Reporting calendar month (1-12)", example = "10")
    private Integer reportingMonth;

    @Schema(description = "Reporting period code (YYYY-MM)", example = "2026-10")
    private String reportingPeriod;

    @Schema(description = "Category-by-category allocation and sales breakdowns")
    private List<CategoryTransparencyDTO> categories = new ArrayList<>();

    @Schema(description = "Total allocated cylinders across all categories with published quotas", example = "7500")
    private Integer totalAllocated = 0;

    @Schema(description = "Total delivered cylinders across all categories in this reporting period", example = "4810")
    private Integer totalDelivered = 0;

    @Schema(description = "Total remaining cylinders balance across all allocated categories", example = "2690")
    private Integer totalRemaining = 0;

    @Schema(description = "Total net adjustments recorded for this period", example = "-50")
    private Integer totalAdjustments = 0;

    @Schema(description = "Overall allocation utilization percentage", example = "64.56")
    private Double overallUtilizationPercentage = 0.0;

    @Schema(description = "Timestamp of the most recent allocation or delivery update in this report", example = "2026-10-10T16:45:00")
    private LocalDateTime lastUpdated;

    @Schema(description = "Indicates whether any official allocations exist for this distributor in this period", example = "true")
    private boolean hasAllocations = false;

    public TransparencyReportDTO() {
    }

    public Long getDistributorId() { return distributorId; }
    public void setDistributorId(Long distributorId) { this.distributorId = distributorId; }

    public String getDistributorName() { return distributorName; }
    public void setDistributorName(String distributorName) { this.distributorName = distributorName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getReportingYear() { return reportingYear; }
    public void setReportingYear(Integer reportingYear) { this.reportingYear = reportingYear; }

    public Integer getReportingMonth() { return reportingMonth; }
    public void setReportingMonth(Integer reportingMonth) { this.reportingMonth = reportingMonth; }

    public String getReportingPeriod() { return reportingPeriod; }
    public void setReportingPeriod(String reportingPeriod) { this.reportingPeriod = reportingPeriod; }

    public List<CategoryTransparencyDTO> getCategories() { return categories; }
    public void setCategories(List<CategoryTransparencyDTO> categories) { this.categories = categories; }

    public Integer getTotalAllocated() { return totalAllocated; }
    public void setTotalAllocated(Integer totalAllocated) { this.totalAllocated = totalAllocated; }

    public Integer getTotalDelivered() { return totalDelivered; }
    public void setTotalDelivered(Integer totalDelivered) { this.totalDelivered = totalDelivered; }

    public Integer getTotalRemaining() { return totalRemaining; }
    public void setTotalRemaining(Integer totalRemaining) { this.totalRemaining = totalRemaining; }

    public Integer getTotalAdjustments() { return totalAdjustments; }
    public void setTotalAdjustments(Integer totalAdjustments) { this.totalAdjustments = totalAdjustments; }

    public Double getOverallUtilizationPercentage() { return overallUtilizationPercentage; }
    public void setOverallUtilizationPercentage(Double overallUtilizationPercentage) { this.overallUtilizationPercentage = overallUtilizationPercentage; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public boolean isHasAllocations() { return hasAllocations; }
    public void setHasAllocations(boolean hasAllocations) { this.hasAllocations = hasAllocations; }
}
