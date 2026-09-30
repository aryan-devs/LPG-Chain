package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for registering or updating a delivery vehicle")
public class VehicleRequestDTO {

    @NotBlank(message = "vehicleNumber is required")
    @Schema(description = "Unique registration number of the vehicle", example = "MH-01-AB-1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String vehicleNumber;

    @NotBlank(message = "vehicleType is required")
    @Schema(description = "Type of vehicle (e.g. LPG_DELIVERY, MINI_TRUCK, TRUCK)", example = "LPG_DELIVERY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String vehicleType;

    @NotNull(message = "capacity is required")
    @Min(value = 1, message = "capacity must be at least 1 cylinder")
    @Schema(description = "Cylinder carrying capacity of the vehicle", example = "30", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer capacity;

    @NotNull(message = "distributorId is required")
    @Schema(description = "Distributor ID to which this vehicle belongs", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long distributorId;

    @Schema(description = "Vehicle operational status (AVAILABLE, ASSIGNED, IN_TRANSIT, INACTIVE)", example = "AVAILABLE")
    private String status;

    @Schema(description = "Associated Traccar GPS device identifier", example = "784521")
    private String traccarDeviceId;

    public VehicleRequestDTO() {
    }

    public VehicleRequestDTO(String vehicleNumber, String vehicleType, Integer capacity, Long distributorId, String status) {
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.capacity = capacity;
        this.distributorId = distributorId;
        this.status = status;
    }

    public VehicleRequestDTO(String vehicleNumber, String vehicleType, Integer capacity, Long distributorId, String status, String traccarDeviceId) {
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.capacity = capacity;
        this.distributorId = distributorId;
        this.status = status;
        this.traccarDeviceId = traccarDeviceId;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Long getDistributorId() {
        return distributorId;
    }

    public void setDistributorId(Long distributorId) {
        this.distributorId = distributorId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTraccarDeviceId() {
        return traccarDeviceId;
    }

    public void setTraccarDeviceId(String traccarDeviceId) {
        this.traccarDeviceId = traccarDeviceId;
    }
}
