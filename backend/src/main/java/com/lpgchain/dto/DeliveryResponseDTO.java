package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Detailed representation of an LPG delivery operation")
public class DeliveryResponseDTO {

    @Schema(description = "Unique delivery identifier", example = "501")
    private Long id;

    @Schema(description = "Associated booking ID", example = "1001")
    private Long bookingId;

    @Schema(description = "Customer ID", example = "101")
    private Long customerId;

    @Schema(description = "Customer name", example = "Rahul Sharma")
    private String customerName;

    @Schema(description = "Assigned driver ID", example = "10")
    private Long driverId;

    @Schema(description = "Assigned driver name", example = "Rajesh Kumar")
    private String driverName;

    @Schema(description = "Assigned vehicle ID", example = "5")
    private Long vehicleId;

    @Schema(description = "Assigned vehicle registration number", example = "MH-01-AB-1234")
    private String vehicleNumber;

    @Schema(description = "Distributor organization ID", example = "1")
    private Long distributorId;

    @Schema(description = "Distributor agency name", example = "City Gas Agency")
    private String distributorName;

    @Schema(description = "Current lifecycle status (ASSIGNED, PICKED_UP, OUT_FOR_DELIVERY, DELIVERED)", example = "ASSIGNED")
    private String status;

    @Schema(description = "Timestamp when delivery was assigned")
    private LocalDateTime assignedAt;

    @Schema(description = "Timestamp when cylinders were picked up from warehouse")
    private LocalDateTime pickedUpAt;

    @Schema(description = "Timestamp when driver started transit for customer delivery")
    private LocalDateTime outForDeliveryAt;

    @Schema(description = "Timestamp when cylinder was handed over to customer")
    private LocalDateTime deliveredAt;

    @Schema(description = "Whether delivery doorstep handover has been initiated by driver", example = "true")
    private Boolean handoverInitiated;

    @Schema(description = "Timestamp when handover OTP was initiated")
    private LocalDateTime handoverInitiatedAt;

    @Schema(description = "Timestamp when active OTP expires")
    private LocalDateTime otpExpiresAt;

    @Schema(description = "Timestamp when delivery handover OTP was successfully verified")
    private LocalDateTime otpVerifiedAt;

    public DeliveryResponseDTO() {
    }

    public DeliveryResponseDTO(Long id, Long bookingId, Long customerId, String customerName,
                               Long driverId, String driverName, Long vehicleId, String vehicleNumber,
                               Long distributorId, String distributorName, String status,
                               LocalDateTime assignedAt, LocalDateTime pickedUpAt,
                               LocalDateTime outForDeliveryAt, LocalDateTime deliveredAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.driverId = driverId;
        this.driverName = driverName;
        this.vehicleId = vehicleId;
        this.vehicleNumber = vehicleNumber;
        this.distributorId = distributorId;
        this.distributorName = distributorName;
        this.status = status;
        this.assignedAt = assignedAt;
        this.pickedUpAt = pickedUpAt;
        this.outForDeliveryAt = outForDeliveryAt;
        this.deliveredAt = deliveredAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getVehicleRegistration() {
        return vehicleNumber;
    }

    public void setVehicleRegistration(String vehicleRegistration) {
        if (this.vehicleNumber == null || this.vehicleNumber.isBlank()) {
            this.vehicleNumber = vehicleRegistration;
        }
    }

    public Long getDistributorId() {
        return distributorId;
    }

    public void setDistributorId(Long distributorId) {
        this.distributorId = distributorId;
    }

    public String getDistributorName() {
        return distributorName;
    }

    public void setDistributorName(String distributorName) {
        this.distributorName = distributorName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(LocalDateTime pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public LocalDateTime getOutForDeliveryAt() {
        return outForDeliveryAt;
    }

    public void setOutForDeliveryAt(LocalDateTime outForDeliveryAt) {
        this.outForDeliveryAt = outForDeliveryAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Boolean getHandoverInitiated() {
        return handoverInitiated;
    }

    public void setHandoverInitiated(Boolean handoverInitiated) {
        this.handoverInitiated = handoverInitiated;
    }

    public LocalDateTime getHandoverInitiatedAt() {
        return handoverInitiatedAt;
    }

    public void setHandoverInitiatedAt(LocalDateTime handoverInitiatedAt) {
        this.handoverInitiatedAt = handoverInitiatedAt;
    }

    public LocalDateTime getOtpExpiresAt() {
        return otpExpiresAt;
    }

    public void setOtpExpiresAt(LocalDateTime otpExpiresAt) {
        this.otpExpiresAt = otpExpiresAt;
    }

    public LocalDateTime getOtpVerifiedAt() {
        return otpVerifiedAt;
    }

    public void setOtpVerifiedAt(LocalDateTime otpVerifiedAt) {
        this.otpVerifiedAt = otpVerifiedAt;
    }
}
