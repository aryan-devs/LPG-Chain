package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BookingResponseDTO — Data transfer object for Booking API responses.
 *
 * Flattens customer and distributor entities into clean identifier and name
 * fields to avoid exposing complete internal JPA entities.
 */
@Data
@Schema(description = "Detailed booking representation returned by Booking API")
public class BookingResponseDTO {

    @Schema(description = "Unique booking identifier", example = "1001")
    private Long id;

    @Schema(description = "Customer user ID", example = "1")
    private Long customerId;

    @Schema(description = "Customer name", example = "Integration User")
    private String customerName;

    @Schema(description = "Assigned distributor organization ID", example = "1")
    private Long distributorId;

    @Schema(description = "Assigned distributor agency name", example = "City Gas Agency")
    private String distributorName;

    @Schema(description = "Delivery destination address", example = "Flat 402, Green Valley Apartments, Mumbai")
    private String deliveryAddress;

    @Schema(description = "Booking creation timestamp", example = "2026-09-14T20:00:00")
    private LocalDateTime bookingDate;

    @Schema(description = "Quantity of cylinders booked", example = "2")
    private Integer quantity;

    @Schema(description = "Booking status lifecycle state (PENDING, CONFIRMED, DELIVERED, CANCELLED)", example = "PENDING")
    private String status;

    @Schema(description = "Cylinder category code", example = "DOMESTIC_14_2")
    private String cylinderCategory;

    @Schema(description = "Nominal cylinder capacity in kg", example = "14.2")
    private Double capacityKg;

    @Schema(description = "Unique booking tracking number", example = "BK-20261018-ABCD")
    private String bookingNumber;

    @Schema(description = "Server-calculated unit price in INR", example = "853.0")
    private Double unitPrice;

    @Schema(description = "Total price before subsidy", example = "853.0")
    private Double totalAmount;

    @Schema(description = "Direct Benefit Transfer subsidy in INR", example = "200.0")
    private Double subsidyAmount;

    @Schema(description = "Final payable amount", example = "653.0")
    private Double netAmount;

    @Schema(description = "Payment status (PENDING, PAID, FAILED, CANCELLED)", example = "PENDING")
    private String paymentStatus;

    @Schema(description = "Payment method (DEMO_QR, CASH_ON_DELIVERY)", example = "DEMO_QR")
    private String paymentMethod;

    @Schema(description = "Payment or transaction reference", example = "TXN-DEMO-123456")
    private String paymentReference;

    public BookingResponseDTO() {
    }

    public BookingResponseDTO(Long id, Long customerId, String customerName,
                              Long distributorId, String distributorName,
                              String deliveryAddress, LocalDateTime bookingDate,
                              Integer quantity, String status) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.distributorId = distributorId;
        this.distributorName = distributorName;
        this.deliveryAddress = deliveryAddress;
        this.bookingDate = bookingDate;
        this.quantity = quantity;
        this.status = status;
        this.cylinderCategory = "DOMESTIC_14_2";
        this.capacityKg = 14.2;
    }

    public BookingResponseDTO(Long id, Long customerId, String customerName,
                              Long distributorId, String distributorName,
                              String deliveryAddress, LocalDateTime bookingDate,
                              Integer quantity, String status,
                              String cylinderCategory, Double capacityKg) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.distributorId = distributorId;
        this.distributorName = distributorName;
        this.deliveryAddress = deliveryAddress;
        this.bookingDate = bookingDate;
        this.quantity = quantity;
        this.status = status;
        this.cylinderCategory = cylinderCategory;
        this.capacityKg = capacityKg;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Long getDistributorId() { return distributorId; }
    public void setDistributorId(Long distributorId) { this.distributorId = distributorId; }

    public String getDistributorName() { return distributorName; }
    public void setDistributorName(String distributorName) { this.distributorName = distributorName; }

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
