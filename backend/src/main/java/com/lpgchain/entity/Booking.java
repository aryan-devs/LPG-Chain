package com.lpgchain.entity;

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
@Table(name = "booking",
        indexes = {
                @Index(name = "idx_booking_customer_id", columnList = "customer_id"),
                @Index(name = "idx_booking_distributor_id", columnList = "distributor_id"),
                @Index(name = "idx_booking_status", columnList = "status"),
                @Index(name = "idx_booking_date", columnList = "bookingDate"),
                @Index(name = "idx_booking_customer_date", columnList = "customer_id, bookingDate")
        }
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distributor_id")
    private Distributor distributor;

    private String deliveryAddress;
    private LocalDateTime bookingDate;
    private Integer quantity;
    private String status;

    @jakarta.persistence.Column(name = "cylinder_category")
    private String cylinderCategory = "DOMESTIC_14_2";

    @jakarta.persistence.Column(name = "capacity_kg")
    private Double capacityKg = 14.2;

    @jakarta.persistence.Column(name = "booking_number")
    private String bookingNumber;

    @jakarta.persistence.Column(name = "unit_price")
    private Double unitPrice;

    @jakarta.persistence.Column(name = "total_amount")
    private Double totalAmount;

    @jakarta.persistence.Column(name = "subsidy_amount")
    private Double subsidyAmount = 0.0;

    @jakarta.persistence.Column(name = "net_amount")
    private Double netAmount;

    @jakarta.persistence.Column(name = "payment_status")
    private String paymentStatus = "PENDING";

    @jakarta.persistence.Column(name = "payment_method")
    private String paymentMethod;

    @jakarta.persistence.Column(name = "payment_reference")
    private String paymentReference;

    public Booking() {
    }

    public Booking(User customer, Distributor distributor, String deliveryAddress,
                   LocalDateTime bookingDate, Integer quantity, String status) {
        this.customer = customer;
        this.distributor = distributor;
        this.deliveryAddress = deliveryAddress;
        this.bookingDate = bookingDate;
        this.quantity = quantity;
        this.status = status;
        this.cylinderCategory = "DOMESTIC_14_2";
        this.capacityKg = 14.2;
    }

    public Booking(Long id, User customer, Distributor distributor, String deliveryAddress,
                   LocalDateTime bookingDate, Integer quantity, String status) {
        this.id = id;
        this.customer = customer;
        this.distributor = distributor;
        this.deliveryAddress = deliveryAddress;
        this.bookingDate = bookingDate;
        this.quantity = quantity;
        this.status = status;
        this.cylinderCategory = "DOMESTIC_14_2";
        this.capacityKg = 14.2;
    }

    public Booking(Long id, User customer, Distributor distributor, String deliveryAddress,
                   LocalDateTime bookingDate, Integer quantity, String status,
                   String cylinderCategory, Double capacityKg) {
        this.id = id;
        this.customer = customer;
        this.distributor = distributor;
        this.deliveryAddress = deliveryAddress;
        this.bookingDate = bookingDate;
        this.quantity = quantity;
        this.status = status;
        this.cylinderCategory = cylinderCategory != null ? cylinderCategory : "DOMESTIC_14_2";
        this.capacityKg = capacityKg != null ? capacityKg : 14.2;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public Distributor getDistributor() { return distributor; }
    public void setDistributor(Distributor distributor) { this.distributor = distributor; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public LocalDateTime getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDateTime bookingDate) { this.bookingDate = bookingDate; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCylinderCategory() { return cylinderCategory; }
    public void setCylinderCategory(String cylinderCategory) { this.cylinderCategory = cylinderCategory; }

    public Double getCapacityKg() { return capacityKg; }
    public void setCapacityKg(Double capacityKg) { this.capacityKg = capacityKg; }

    public String getBookingNumber() { return bookingNumber; }
    public void setBookingNumber(String bookingNumber) { this.bookingNumber = bookingNumber; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public Double getSubsidyAmount() { return subsidyAmount; }
    public void setSubsidyAmount(Double subsidyAmount) { this.subsidyAmount = subsidyAmount; }

    public Double getNetAmount() { return netAmount; }
    public void setNetAmount(Double netAmount) { this.netAmount = netAmount; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
}
