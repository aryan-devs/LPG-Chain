package com.lpgchain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Distributor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String agencyName;
    private String licenceNumber;
    private String contactNumber;
    private String address;
    private String status;
    private String passkey;
    private LocalDateTime passkeyExpiry;
    private boolean passkeyUsed;

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
}
