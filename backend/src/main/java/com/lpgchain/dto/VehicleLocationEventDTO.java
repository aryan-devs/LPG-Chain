package com.lpgchain.dto;

import java.time.LocalDateTime;

/**
 * VehicleLocationEventDTO — Dedicated real-time telemetry event payload broadcast
 * over STOMP topic destinations (e.g. /topic/deliveries/{deliveryId}/tracking).
 *
 * Contains only non-sensitive GPS coordinates and delivery context required for map rendering.
 * Does not expose passwords, JWTs, Traccar credentials, or internal entity relationships.
 */
public class VehicleLocationEventDTO {

    private Long deliveryId;
    private Long vehicleId;
    private String vehicleNumber;
    private Double latitude;
    private Double longitude;
    private Double speed;
    private Double heading;
    private LocalDateTime recordedAt;

    public VehicleLocationEventDTO() {
    }

    public VehicleLocationEventDTO(Long deliveryId, Long vehicleId, String vehicleNumber,
                                 Double latitude, Double longitude, Double speed,
                                 Double heading, LocalDateTime recordedAt) {
        this.deliveryId = deliveryId;
        this.vehicleId = vehicleId;
        this.vehicleNumber = vehicleNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.heading = heading;
        this.recordedAt = recordedAt;
    }

    public Long getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId) {
        this.deliveryId = deliveryId;
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

    @Override
    public String toString() {
        return "VehicleLocationEventDTO{" +
                "deliveryId=" + deliveryId +
                ", vehicleId=" + vehicleId +
                ", vehicleNumber='" + vehicleNumber + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", speed=" + speed +
                ", heading=" + heading +
                ", recordedAt=" + recordedAt +
                '}';
    }
}
