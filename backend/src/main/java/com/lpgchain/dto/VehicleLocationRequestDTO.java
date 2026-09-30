package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "Request payload for ingesting a vehicle GPS location record")
public class VehicleLocationRequestDTO {

    @Schema(description = "Optional vehicle ID (defaults to URL path vehicleId)", example = "1")
    private Long vehicleId;

    @NotNull(message = "latitude is required")
    @DecimalMin(value = "-90.0", message = "latitude must be between -90.0 and 90.0")
    @DecimalMax(value = "90.0", message = "latitude must be between -90.0 and 90.0")
    @Schema(description = "Latitude in decimal degrees (-90.0 to 90.0)", example = "18.6298", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double latitude;

    @NotNull(message = "longitude is required")
    @DecimalMin(value = "-180.0", message = "longitude must be between -180.0 and 180.0")
    @DecimalMax(value = "180.0", message = "longitude must be between -180.0 and 180.0")
    @Schema(description = "Longitude in decimal degrees (-180.0 to 180.0)", example = "73.7997", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double longitude;

    @NotNull(message = "speed is required")
    @DecimalMin(value = "0.0", message = "speed must not be negative")
    @Schema(description = "Current vehicle speed in km/h (must be non-negative)", example = "32.5", minimum = "0.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double speed;

    @NotNull(message = "heading is required")
    @DecimalMin(value = "0.0", message = "heading must be between 0.0 and 360.0 degrees")
    @DecimalMax(value = "359.9999", message = "heading must be less than 360.0 degrees")
    @Schema(description = "Vehicle heading/course direction in degrees [0.0, 360.0)", example = "180.0", minimum = "0.0", maximum = "359.9999", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double heading;

    @NotNull(message = "recordedAt is required")
    @Schema(description = "Timestamp when the GPS coordinate was recorded by device/client", example = "2026-09-17T18:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime recordedAt;

    public VehicleLocationRequestDTO() {
    }

    public VehicleLocationRequestDTO(Double latitude, Double longitude, Double speed, Double heading, LocalDateTime recordedAt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.heading = heading;
        this.recordedAt = recordedAt;
    }

    public VehicleLocationRequestDTO(Long vehicleId, Double latitude, Double longitude, Double speed, Double heading, LocalDateTime recordedAt) {
        this.vehicleId = vehicleId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.heading = heading;
        this.recordedAt = recordedAt;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
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
