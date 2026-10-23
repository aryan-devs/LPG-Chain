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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * CylinderInventory — Real physical cylinder inventory count stored at a distributor's warehouse depot.
 * Separated strictly from monthly OMC quota allocations and sales transparency balances.
 */
@Entity
@Table(name = "cylinder_inventory",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_distributor_category_inventory",
                        columnNames = {"distributor_id", "cylinder_category"}
                )
        },
        indexes = {
                @Index(name = "idx_inv_distributor", columnList = "distributor_id"),
                @Index(name = "idx_inv_category", columnList = "cylinder_category")
        }
)
public class CylinderInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distributor_id", nullable = false)
    private Distributor distributor;

    @Column(name = "cylinder_category", nullable = false, length = 50)
    private String cylinderCategory; // DOMESTIC_14_2, DOMESTIC_5_0, COMMERCIAL_19_0

    @Column(name = "capacity_kg", nullable = false)
    private Double capacityKg;

    @Column(name = "filled_stock", nullable = false)
    private Integer filledStock = 0;

    @Column(name = "empty_stock", nullable = false)
    private Integer emptyStock = 0;

    @Column(name = "reserved_stock", nullable = false)
    private Integer reservedStock = 0;

    @Column(name = "damaged_stock", nullable = false)
    private Integer damagedStock = 0;

    @Column(name = "last_audited_at", nullable = false)
    private LocalDateTime lastAuditedAt;

    @Column(name = "last_audited_by", length = 100)
    private String lastAuditedBy;

    public CylinderInventory() {
        this.lastAuditedAt = LocalDateTime.now();
    }

    public CylinderInventory(Distributor distributor, String cylinderCategory, Double capacityKg,
                             Integer filledStock, Integer emptyStock, Integer reservedStock,
                             Integer damagedStock, String lastAuditedBy) {
        this.distributor = distributor;
        this.cylinderCategory = cylinderCategory;
        this.capacityKg = capacityKg;
        this.filledStock = filledStock != null ? filledStock : 0;
        this.emptyStock = emptyStock != null ? emptyStock : 0;
        this.reservedStock = reservedStock != null ? reservedStock : 0;
        this.damagedStock = damagedStock != null ? damagedStock : 0;
        this.lastAuditedBy = lastAuditedBy;
        this.lastAuditedAt = LocalDateTime.now();
    }

    public Integer getAvailableStock() {
        int avail = (filledStock != null ? filledStock : 0) - (reservedStock != null ? reservedStock : 0);
        return Math.max(0, avail);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Distributor getDistributor() { return distributor; }
    public void setDistributor(Distributor distributor) { this.distributor = distributor; }

    public String getCylinderCategory() { return cylinderCategory; }
    public void setCylinderCategory(String cylinderCategory) { this.cylinderCategory = cylinderCategory; }

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

    public LocalDateTime getLastAuditedAt() { return lastAuditedAt; }
    public void setLastAuditedAt(LocalDateTime lastAuditedAt) { this.lastAuditedAt = lastAuditedAt; }

    public String getLastAuditedBy() { return lastAuditedBy; }
    public void setLastAuditedBy(String lastAuditedBy) { this.lastAuditedBy = lastAuditedBy; }
}
