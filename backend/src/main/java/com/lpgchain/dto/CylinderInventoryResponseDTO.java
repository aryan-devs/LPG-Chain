package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Category-wise physical cylinder inventory status at a distributor depot")
public class CylinderInventoryResponseDTO {

    @Schema(description = "Inventory record identifier", example = "1")
    private Long id;

    @Schema(description = "Distributor agency ID", example = "1")
    private Long distributorId;

    @Schema(description = "Distributor agency trade name", example = "City Gas Agency")
    private String distributorName;

    @Schema(description = "Cylinder category code", example = "DOMESTIC_14_2")
    private String cylinderCategory;

    @Schema(description = "Cylinder display name", example = "Domestic 14.2 kg")
    private String categoryDisplayName;

    @Schema(description = "Capacity in kg", example = "14.2")
    private Double capacityKg;

    @Schema(description = "Filled physical cylinders in depot", example = "420")
    private Integer filledStock;

    @Schema(description = "Empty cylinders collected in depot", example = "115")
    private Integer emptyStock;

    @Schema(description = "Cylinders reserved for accepted/scheduled orders", example = "35")
    private Integer reservedStock;

    @Schema(description = "Damaged or quarantined cylinders awaiting inspection", example = "4")
    private Integer damagedStock;

    @Schema(description = "Available cylinders ready for dispatch (filled minus reserved)", example = "385")
    private Integer availableStock;

    @Schema(description = "Timestamp of the last physical stock audit/adjustment", example = "2026-10-11T09:30:00")
    private LocalDateTime lastAuditedAt;

    @Schema(description = "User who performed the last stock adjustment", example = "distributor@agency.example")
    private String lastAuditedBy;

    public CylinderInventoryResponseDTO() {}

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

    public Integer getFilledStock() { return filledStock; }
    public void setFilledStock(Integer filledStock) { this.filledStock = filledStock; }

    public Integer getEmptyStock() { return emptyStock; }
    public void setEmptyStock(Integer emptyStock) { this.emptyStock = emptyStock; }

    public Integer getReservedStock() { return reservedStock; }
    public void setReservedStock(Integer reservedStock) { this.reservedStock = reservedStock; }

    public Integer getDamagedStock() { return damagedStock; }
    public void setDamagedStock(Integer damagedStock) { this.damagedStock = damagedStock; }

    public Integer getAvailableStock() { return availableStock; }
    public void setAvailableStock(Integer availableStock) { this.availableStock = availableStock; }

    public LocalDateTime getLastAuditedAt() { return lastAuditedAt; }
    public void setLastAuditedAt(LocalDateTime lastAuditedAt) { this.lastAuditedAt = lastAuditedAt; }

    public String getLastAuditedBy() { return lastAuditedBy; }
    public void setLastAuditedBy(String lastAuditedBy) { this.lastAuditedBy = lastAuditedBy; }
}
