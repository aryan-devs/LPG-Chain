package com.lpgchain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * AllocationAdjustment — Auditable correction, return, surrender, or emergency reallocation
 * recorded against an official cylinder allocation.
 */
@Entity
@Table(name = "allocation_adjustments",
        indexes = {
                @Index(name = "idx_adj_allocation_id", columnList = "allocation_id"),
                @Index(name = "idx_adj_type", columnList = "adjustment_type")
        }
)
public class AllocationAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id", nullable = false)
    private CylinderAllocation allocation;

    @Column(name = "adjustment_type", nullable = false, length = 50)
    private String adjustmentType; // e.g. "CORRECTION", "RETURN", "DAMAGE_WRITE_OFF", "REALLOCATION"

    @Column(name = "adjusted_quantity", nullable = false)
    private Integer adjustedQuantity; // positive (adds to allocation) or negative (deducts)

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber; // e.g. "ADJ-2026-10-001"

    @Column(name = "adjusted_by", length = 100)
    private String adjustedBy;

    @Column(name = "adjusted_at", nullable = false)
    private LocalDateTime adjustedAt;

    public AllocationAdjustment() {
    }

    public AllocationAdjustment(CylinderAllocation allocation, String adjustmentType,
                                Integer adjustedQuantity, String reason,
                                String referenceNumber, String adjustedBy) {
        this.allocation = allocation;
        this.adjustmentType = adjustmentType;
        this.adjustedQuantity = adjustedQuantity;
        this.reason = reason;
        this.referenceNumber = referenceNumber;
        this.adjustedBy = adjustedBy;
        this.adjustedAt = LocalDateTime.now();
    }

    public AllocationAdjustment(Long id, CylinderAllocation allocation, String adjustmentType,
                                Integer adjustedQuantity, String reason,
                                String referenceNumber, String adjustedBy, LocalDateTime adjustedAt) {
        this.id = id;
        this.allocation = allocation;
        this.adjustmentType = adjustmentType;
        this.adjustedQuantity = adjustedQuantity;
        this.reason = reason;
        this.referenceNumber = referenceNumber;
        this.adjustedBy = adjustedBy;
        this.adjustedAt = adjustedAt != null ? adjustedAt : LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CylinderAllocation getAllocation() { return allocation; }
    public void setAllocation(CylinderAllocation allocation) { this.allocation = allocation; }

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
