package com.lpgchain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "distributor",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_distributor_licence", columnNames = {"licence_number"})
        },
        indexes = {
                @Index(name = "idx_distributor_licence", columnList = "licence_number"),
                @Index(name = "idx_distributor_status", columnList = "status"),
                @Index(name = "idx_distributor_pin_code", columnList = "pin_code"),
                @Index(name = "idx_distributor_city", columnList = "city")
        }
)
public class Distributor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String agencyName;

    @Column(name = "licence_number", unique = true)
    private String licenceNumber;
    private String contactNumber;
    private String address;
    private String status;
    private String passkey;
    private LocalDateTime passkeyExpiry;
    private boolean passkeyUsed;

    @Column(name = "pin_code")
    private String pinCode;

    private String city;

    private String state;

    private Double latitude;

    private Double longitude;

    @Column(name = "supported_services")
    private String supportedServices;

    @Column(name = "operating_hours")
    private String operatingHours;

    public Distributor() {
    }

    public Distributor(String agencyName, String licenceNumber, String contactNumber,
                       String address, String status, String passkey,
                       LocalDateTime passkeyExpiry, boolean passkeyUsed) {
        this.agencyName = agencyName;
        this.licenceNumber = licenceNumber;
        this.contactNumber = contactNumber;
        this.address = address;
        this.status = status;
        this.passkey = passkey;
        this.passkeyExpiry = passkeyExpiry;
        this.passkeyUsed = passkeyUsed;
    }

    public Distributor(Long id, String agencyName, String licenceNumber, String contactNumber,
                       String address, String status, String passkey,
                       LocalDateTime passkeyExpiry, boolean passkeyUsed) {
        this.id = id;
        this.agencyName = agencyName;
        this.licenceNumber = licenceNumber;
        this.contactNumber = contactNumber;
        this.address = address;
        this.status = status;
        this.passkey = passkey;
        this.passkeyExpiry = passkeyExpiry;
        this.passkeyUsed = passkeyUsed;
    }

    public Distributor(Long id, String agencyName, String licenceNumber, String contactNumber,
                       String address, String status, String passkey,
                       LocalDateTime passkeyExpiry, boolean passkeyUsed,
                       String pinCode, String city, String state,
                       Double latitude, Double longitude,
                       String supportedServices, String operatingHours) {
        this(id, agencyName, licenceNumber, contactNumber, address, status, passkey, passkeyExpiry, passkeyUsed);
        this.pinCode = pinCode;
        this.city = city;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
        this.supportedServices = supportedServices;
        this.operatingHours = operatingHours;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgencyName() { return agencyName; }
    public void setAgencyName(String agencyName) { this.agencyName = agencyName; }

    public String getLicenceNumber() { return licenceNumber; }
    public void setLicenceNumber(String licenceNumber) { this.licenceNumber = licenceNumber; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPasskey() { return passkey; }
    public void setPasskey(String passkey) { this.passkey = passkey; }

    public LocalDateTime getPasskeyExpiry() { return passkeyExpiry; }
    public void setPasskeyExpiry(LocalDateTime passkeyExpiry) { this.passkeyExpiry = passkeyExpiry; }

    public boolean isPasskeyUsed() { return passkeyUsed; }
    public void setPasskeyUsed(boolean passkeyUsed) { this.passkeyUsed = passkeyUsed; }

    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getSupportedServices() { return supportedServices; }
    public void setSupportedServices(String supportedServices) { this.supportedServices = supportedServices; }

    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }
}
