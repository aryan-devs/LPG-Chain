package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "Request payload for creating a new LPG cylinder booking")
public class BookingRequestDTO {

    @Schema(description = "Customer ID creating the booking (optional for authenticated customers; automatically bound from authentication)", example = "1")
    private Long customerId;

    @Schema(description = "Optional assigned distributor ID (can be assigned later by administrator)", example = "1")
    private Long distributorId;

    @NotBlank(message = "deliveryAddress is required")
    @Schema(description = "Physical address for cylinder delivery", example = "Flat 402, Green Valley Apartments, Mumbai", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deliveryAddress;

    @Schema(description = "Scheduled booking timestamp (defaults to current time if null)", example = "2026-09-15T10:00:00")
    private LocalDateTime bookingDate;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    @Schema(description = "Number of LPG cylinders requested", example = "2", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;

    @Schema(description = "Cylinder category (DOMESTIC_14_2, DOMESTIC_5_0, COMMERCIAL_19_0)", example = "DOMESTIC_14_2")
    private String cylinderCategory;

    @Schema(description = "Nominal cylinder capacity in kg", example = "14.2")
    private Double capacityKg;

    @Schema(description = "Payment method: DEMO_QR or CASH_ON_DELIVERY", example = "DEMO_QR")
    private String paymentMethod;

    @Schema(description = "Optional saved address ID to link delivery address", example = "1")
    private Long savedAddressId;

    public BookingRequestDTO() {
    }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getDistributorId() { return distributorId; }
    public void setDistributorId(Long distributorId) { this.distributorId = distributorId; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public LocalDateTime getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDateTime bookingDate) { this.bookingDate = bookingDate; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getCylinderCategory() { return cylinderCategory; }
    public void setCylinderCategory(String cylinderCategory) { this.cylinderCategory = cylinderCategory; }

    public Double getCapacityKg() { return capacityKg; }
    public void setCapacityKg(Double capacityKg) { this.capacityKg = capacityKg; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Long getSavedAddressId() { return savedAddressId; }
    public void setSavedAddressId(Long savedAddressId) { this.savedAddressId = savedAddressId; }
}
