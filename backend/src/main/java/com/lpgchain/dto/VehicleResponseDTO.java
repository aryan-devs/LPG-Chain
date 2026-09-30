package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detailed representation of a delivery vehicle")
public class VehicleResponseDTO {

    @Schema(description = "Unique vehicle identifier", example = "5")
    private Long id;

    @Schema(description = "Unique registration vehicle number", example = "MH-01-AB-1234")
    private String vehicleNumber;

    @Schema(description = "Type of vehicle", example = "LPG_DELIVERY")
    private String vehicleType;

    @Schema(description = "Cylinder carrying capacity", example = "30")
    private Integer capacity;

    @Schema(description = "Operational status", example = "AVAILABLE")
    private String status;

    @Schema(description = "Distributor organization ID", example = "1")
    private Long distributorId;

    @Schema(description = "Distributor agency name", example = "City Gas Agency")
    private String distributorName;

    @Schema(description = "Associated Traccar GPS device identifier", example = "784521")
    private String traccarDeviceId;

    public VehicleResponseDTO() {
    }

    public VehicleResponseDTO(Long id, String vehicleNumber, String vehicleType, Integer capacity,
                              String status, Long distributorId, String distributorName) {
        this.id = id;
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.capacity = capacity;
        this.status = status;
        this.distributorId = distributorId;
        this.distributorName = distributorName;
    }

    public VehicleResponseDTO(Long id, String vehicleNumber, String vehicleType, Integer capacity,
                              String status, Long distributorId, String distributorName, String traccarDeviceId) {
        this.id = id;
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.capacity = capacity;
        this.status = status;
        this.distributorId = distributorId;
        this.distributorName = distributorName;
        this.traccarDeviceId = traccarDeviceId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getTraccarDeviceId() {
        return traccarDeviceId;
    }

    public void setTraccarDeviceId(String traccarDeviceId) {
        this.traccarDeviceId = traccarDeviceId;
    }
}
