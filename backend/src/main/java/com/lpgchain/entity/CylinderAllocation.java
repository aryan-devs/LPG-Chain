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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CylinderAllocation — Official cylinder quota allocated to a distributor for a specific category
 * and reporting period (month/year).
 */
@Entity
@Table(name = "cylinder_allocation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_distributor_category_period",
                        columnNames = {"distributor_id", "cylinder_category", "reporting_year", "reporting_month"}
                )
        },
        indexes = {
                @Index(name = "idx_alloc_distributor", columnList = "distributor_id"),
                @Index(name = "idx_alloc_period", columnList = "reporting_year, reporting_month"),
                @Index(name = "idx_alloc_category", columnList = "cylinder_category"),
                @Index(name = "idx_alloc_dist_period", columnList = "distributor_id, reporting_year, reporting_month")
        }
)
public class CylinderAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distributor_id", nullable = false)
    private Distributor distributor;

    @Column(name = "cylinder_category", nullable = false, length = 50)
    private String cylinderCategory; // e.g. "DOMESTIC_14_2", "DOMESTIC_5_0", "COMMERCIAL_19_0"

    @Column(name = "capacity_kg", nullable = false)
    private Double capacityKg; // e.g. 14.2, 5.0, 19.0

    @Column(name = "allocated_quantity", nullable = false)
    private Integer allocatedQuantity;

    @Column(name = "reporting_year", nullable = false)
    private Integer reportingYear;

    @Column(name = "reporting_month", nullable = false)
    private Integer reportingMonth;

    @Column(name = "reporting_period", nullable = false, length = 7)
    private String reportingPeriod; // "YYYY-MM" format e.g. "2026-10"

    @Column(name = "allocation_date", nullable = false)
    private LocalDate allocationDate;

    @Column(name = "source_reference", length = 100)
    private String sourceReference; // e.g. "MOPNG-Q4-REF-101", "OMC Depot Indent #405"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public CylinderAllocation() {
    }

    public CylinderAllocation(Distributor distributor, String cylinderCategory, Double capacityKg,
                              Integer allocatedQuantity, Integer reportingYear, Integer reportingMonth,
                              LocalDate allocationDate, String sourceReference, String createdBy) {
        this.distributor = distributor;
        this.cylinderCategory = cylinderCategory;
        this.capacityKg = capacityKg;
        this.allocatedQuantity = allocatedQuantity;
        this.reportingYear = reportingYear;
        this.reportingMonth = reportingMonth;
        this.reportingPeriod = String.format("%04d-%02d", reportingYear, reportingMonth);
        this.allocationDate = allocationDate != null ? allocationDate : LocalDate.now();
        this.sourceReference = sourceReference;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }

    public CylinderAllocation(Long id, Distributor distributor, String cylinderCategory, Double capacityKg,
                              Integer allocatedQuantity, Integer reportingYear, Integer reportingMonth,
                              LocalDate allocationDate, String sourceReference, LocalDateTime createdAt,
                              LocalDateTime updatedAt, String createdBy, String updatedBy) {
        this.id = id;
        this.distributor = distributor;
        this.cylinderCategory = cylinderCategory;
        this.capacityKg = capacityKg;
        this.allocatedQuantity = allocatedQuantity;
        this.reportingYear = reportingYear;
        this.reportingMonth = reportingMonth;
        this.reportingPeriod = String.format("%04d-%02d", reportingYear, reportingMonth);
        this.allocationDate = allocationDate != null ? allocationDate : LocalDate.now();
        this.sourceReference = sourceReference;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Distributor getDistributor() { return distributor; }
    public void setDistributor(Distributor distributor) { this.distributor = distributor; }

    public String getCylinderCategory() { return cylinderCategory; }
    public void setCylinderCategory(String cylinderCategory) { this.cylinderCategory = cylinderCategory; }

    public Double getCapacityKg() { return capacityKg; }
    public void setCapacityKg(Double capacityKg) { this.capacityKg = capacityKg; }

    public Integer getAllocatedQuantity() { return allocatedQuantity; }
    public void setAllocatedQuantity(Integer allocatedQuantity) { this.allocatedQuantity = allocatedQuantity; }

    public Integer getReportingYear() { return reportingYear; }
    public void setReportingYear(Integer reportingYear) {
        this.reportingYear = reportingYear;
        if (this.reportingMonth != null) {
            this.reportingPeriod = String.format("%04d-%02d", reportingYear, this.reportingMonth);
        }
    }

    public Integer getReportingMonth() { return reportingMonth; }
    public void setReportingMonth(Integer reportingMonth) {
        this.reportingMonth = reportingMonth;
        if (this.reportingYear != null) {
            this.reportingPeriod = String.format("%04d-%02d", this.reportingYear, reportingMonth);
        }
    }

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
