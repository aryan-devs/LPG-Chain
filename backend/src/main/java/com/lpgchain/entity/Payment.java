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

@Entity
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_payments_booking_id", columnList = "booking_id"),
                @Index(name = "idx_payments_customer_id", columnList = "customer_id"),
                @Index(name = "idx_payments_txn_ref", columnList = "transaction_reference")
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "subsidy_amount")
    private Double subsidyAmount = 0.0;

    @Column(name = "net_amount", nullable = false)
    private Double netAmount;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // DEMO_QR, CASH_ON_DELIVERY

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "PENDING"; // PENDING, PAID, FAILED, CANCELLED

    @Column(name = "transaction_reference", unique = true, nullable = false)
    private String transactionReference;

    @Column(name = "is_simulated")
    private Boolean isSimulated = false;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "collected_by")
    private String collectedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Payment() {
    }

    public Payment(Booking booking, User customer, Double amount, Double subsidyAmount, Double netAmount,
                   String paymentMethod, String paymentStatus, String transactionReference, Boolean isSimulated) {
        this.booking = booking;
        this.customer = customer;
        this.amount = amount;
        this.subsidyAmount = subsidyAmount != null ? subsidyAmount : 0.0;
        this.netAmount = netAmount != null ? netAmount : amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus != null ? paymentStatus : "PENDING";
        this.transactionReference = transactionReference;
        this.isSimulated = isSimulated != null ? isSimulated : false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Double getSubsidyAmount() { return subsidyAmount; }
    public void setSubsidyAmount(Double subsidyAmount) { this.subsidyAmount = subsidyAmount; }

    public Double getNetAmount() { return netAmount; }
    public void setNetAmount(Double netAmount) { this.netAmount = netAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public Boolean getIsSimulated() { return isSimulated; }
    public void setIsSimulated(Boolean isSimulated) { this.isSimulated = isSimulated; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public String getCollectedBy() { return collectedBy; }
    public void setCollectedBy(String collectedBy) { this.collectedBy = collectedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
