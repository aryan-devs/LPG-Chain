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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "vehicles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vehicles_number", columnNames = {"vehicleNumber"})
        },
        indexes = {
                @Index(name = "idx_vehicles_distributor_id", columnList = "distributor_id"),
                @Index(name = "idx_vehicles_status", columnList = "status"),
                @Index(name = "idx_vehicles_number", columnList = "vehicleNumber"),
                @Index(name = "idx_vehicles_traccar_device", columnList = "traccar_device_id")
        }
)
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String vehicleNumber;

    @Column(nullable = false)
    private String vehicleType;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private String status = "AVAILABLE";

    @Column(name = "traccar_device_id")
    private String traccarDeviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distributor_id", nullable = false)
    private Distributor distributor;

    public Vehicle() {
    }

    public Vehicle(String vehicleNumber, String vehicleType, Integer capacity, String status, Distributor distributor) {
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.capacity = capacity;
        this.status = status != null ? status : "AVAILABLE";
        this.distributor = distributor;
    }

    public Vehicle(Long id, String vehicleNumber, String vehicleType, Integer capacity, String status, Distributor distributor) {
        this.id = id;
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.capacity = capacity;
        this.status = status != null ? status : "AVAILABLE";
        this.distributor = distributor;
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

    public Distributor getDistributor() {
        return distributor;
    }

    public void setDistributor(Distributor distributor) {
        this.distributor = distributor;
    }

    public String getTraccarDeviceId() {
        return traccarDeviceId;
    }

    public void setTraccarDeviceId(String traccarDeviceId) {
        this.traccarDeviceId = traccarDeviceId;
    }
}
