package com.lpgchain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries",
        indexes = {
                @Index(name = "idx_deliveries_booking_id", columnList = "booking_id"),
                @Index(name = "idx_deliveries_distributor_id", columnList = "distributor_id"),
                @Index(name = "idx_deliveries_driver_id", columnList = "driver_id"),
                @Index(name = "idx_deliveries_vehicle_id", columnList = "vehicle_id"),
                @Index(name = "idx_deliveries_status", columnList = "status")
        }
)
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distributor_id", nullable = false)
    private Distributor distributor;

    @Column(nullable = false)
    private String status = "ASSIGNED";

    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime outForDeliveryAt;
    private LocalDateTime deliveredAt;

    @Column(name = "otp_hash", length = 100)
    private String otpHash;

    private LocalDateTime otpExpiresAt;

    @Column(name = "otp_attempts")
    private Integer otpAttempts = 0;

    @Column(name = "otp_max_attempts")
    private Integer otpMaxAttempts = 3;

    private LocalDateTime otpGeneratedAt;

    private LocalDateTime otpVerifiedAt;

    public Delivery() {
    }

    public Delivery(Booking booking, Driver driver, Vehicle vehicle, Distributor distributor, String status) {
        this.booking = booking;
        this.driver = driver;
        this.vehicle = vehicle;
        this.distributor = distributor;
        this.status = status != null ? status : "ASSIGNED";
        this.assignedAt = LocalDateTime.now();
    }

    public Delivery(Long id, Booking booking, Driver driver, Vehicle vehicle, Distributor distributor,
                    String status, LocalDateTime assignedAt, LocalDateTime pickedUpAt,
                    LocalDateTime outForDeliveryAt, LocalDateTime deliveredAt) {
        this.id = id;
        this.booking = booking;
        this.driver = driver;
        this.vehicle = vehicle;
        this.distributor = distributor;
        this.status = status != null ? status : "ASSIGNED";
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

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Distributor getDistributor() {
        return distributor;
    }

    public void setDistributor(Distributor distributor) {
        this.distributor = distributor;
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

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public LocalDateTime getOtpExpiresAt() {
        return otpExpiresAt;
    }

    public void setOtpExpiresAt(LocalDateTime otpExpiresAt) {
        this.otpExpiresAt = otpExpiresAt;
    }

    public Integer getOtpAttempts() {
        return otpAttempts;
    }

    public void setOtpAttempts(Integer otpAttempts) {
        this.otpAttempts = otpAttempts;
    }

    public Integer getOtpMaxAttempts() {
        return otpMaxAttempts;
    }

    public void setOtpMaxAttempts(Integer otpMaxAttempts) {
        this.otpMaxAttempts = otpMaxAttempts;
    }

    public LocalDateTime getOtpGeneratedAt() {
        return otpGeneratedAt;
    }

    public void setOtpGeneratedAt(LocalDateTime otpGeneratedAt) {
        this.otpGeneratedAt = otpGeneratedAt;
    }

    public LocalDateTime getOtpVerifiedAt() {
        return otpVerifiedAt;
    }

    public void setOtpVerifiedAt(LocalDateTime otpVerifiedAt) {
        this.otpVerifiedAt = otpVerifiedAt;
    }
}
