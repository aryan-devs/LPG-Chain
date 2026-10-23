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
 * InventoryMovement — Auditable log of physical stock adjustments (receipts, delivery dispatches,
 * empty cylinder collections, damage write-offs, physical count reconciliations).
 */
@Entity
@Table(name = "inventory_movements",
        indexes = {
                @Index(name = "idx_move_inventory", columnList = "inventory_id"),
                @Index(name = "idx_move_type", columnList = "movement_type"),
                @Index(name = "idx_move_date", columnList = "performed_at")
        }
)
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private CylinderInventory inventory;

    @Column(name = "movement_type", nullable = false, length = 50)
    private String movementType; // STOCK_RECEIPT, DISPATCH, EMPTY_RETURN, DAMAGE_QUARANTINE, AUDIT_ADJUSTMENT

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    public InventoryMovement() {
        this.performedAt = LocalDateTime.now();
    }

    public InventoryMovement(CylinderInventory inventory, String movementType, Integer quantityChange,
                             String reason, String referenceNumber, String performedBy) {
        this.inventory = inventory;
        this.movementType = movementType;
        this.quantityChange = quantityChange;
        this.reason = reason;
        this.referenceNumber = referenceNumber;
        this.performedBy = performedBy;
        this.performedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CylinderInventory getInventory() { return inventory; }
    public void setInventory(CylinderInventory inventory) { this.inventory = inventory; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }
}
