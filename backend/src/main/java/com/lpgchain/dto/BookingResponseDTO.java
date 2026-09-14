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
}
