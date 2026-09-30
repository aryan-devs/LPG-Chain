package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for creating and assigning a delivery to a booking")
public class DeliveryRequestDTO {

    @NotNull(message = "bookingId is required")
    @Schema(description = "ID of the booking to deliver", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bookingId;

    @NotNull(message = "driverId is required")
    @Schema(description = "ID of the assigned driver", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long driverId;

    @NotNull(message = "vehicleId is required")
    @Schema(description = "ID of the assigned delivery vehicle", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long vehicleId;

    @Schema(description = "Optional distributor ID (defaults to booking distributor; if provided must match booking distributor)", example = "1")
    private Long distributorId;

    public DeliveryRequestDTO() {
    }

    public DeliveryRequestDTO(Long bookingId, Long driverId, Long vehicleId, Long distributorId) {
        this.bookingId = bookingId;
        this.driverId = driverId;
        this.vehicleId = vehicleId;
        this.distributorId = distributorId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getDistributorId() {
        return distributorId;
    }

    public void setDistributorId(Long distributorId) {
        this.distributorId = distributorId;
    }
}
