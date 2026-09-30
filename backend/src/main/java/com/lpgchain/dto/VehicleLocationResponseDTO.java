package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Detailed GPS location and telemetry data for a vehicle")
public class VehicleLocationResponseDTO {

    @Schema(description = "Location record ID", example = "101")
    private Long id;

    @Schema(description = "Vehicle ID", example = "1")
    private Long vehicleId;

    @Schema(description = "Vehicle registration number", example = "MH-12-AB-1234")
    private String vehicleNumber;

    @Schema(description = "Latitude in decimal degrees", example = "18.6298")
    private Double latitude;

    @Schema(description = "Longitude in decimal degrees", example = "73.7997")
    private Double longitude;

    @Schema(description = "Speed in km/h", example = "32.5")
    private Double speed;

    @Schema(description = "Heading/course direction in degrees [0.0, 360.0)", example = "180.0")
    private Double heading;

    @Schema(description = "Timestamp when the GPS coordinate was recorded", example = "2026-09-17T18:30:00")
    private LocalDateTime recordedAt;

    public VehicleLocationResponseDTO() {
    }

    public VehicleLocationResponseDTO(Long id, Long vehicleId, String vehicleNumber, Double latitude,
                                      Double longitude, Double speed, Double heading, LocalDateTime recordedAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.vehicleNumber = vehicleNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.heading = heading;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
