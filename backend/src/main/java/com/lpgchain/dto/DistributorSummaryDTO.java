package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DistributorSummaryDTO — Sanitized public response representation for LPG distributor agencies.
 * Strictly excludes sensitive internal credentials (passkeys, expiry, passwords, internal secrets).
 */
@Schema(description = "Sanitized public summary of an LPG distributor agency")
public class DistributorSummaryDTO {

    @Schema(description = "Distributor unique identifier", example = "1")
    private Long id;

    @Schema(description = "Agency trade name", example = "City Gas Agency")
    private String agencyName;

    @Schema(description = "Public contact phone number", example = "+91 98765 43210")
    private String contactNumber;

    @Schema(description = "Physical street address", example = "12 Market Street")
    private String address;

    @Schema(description = "Postal PIN code", example = "400001")
    private String pinCode;

    @Schema(description = "City", example = "Mumbai")
    private String city;

    @Schema(description = "State or territory", example = "Maharashtra")
    private String state;

    @Schema(description = "Latitude coordinate (WGS84)", example = "18.9220")
    private Double latitude;

    @Schema(description = "Longitude coordinate (WGS84)", example = "72.8347")
    private Double longitude;

    @Schema(description = "Supported customer services (DOMESTIC, COMMERCIAL, BOTH)", example = "BOTH")
    private String supportedServices;

    @Schema(description = "Verified operating hours schedule if available", example = "Mon-Sat: 09:00 - 18:00")
    private String operatingHours;

    @Schema(description = "Network partner classification", example = "LPG_CHAIN_PARTNER")
    private String networkType;

    @Schema(description = "Agency registration status", example = "ACTIVE")
    private String status;

    public DistributorSummaryDTO() {
    }

    public DistributorSummaryDTO(Long id, String agencyName, String contactNumber,
                                 String address, String pinCode, String city, String state,
                                 Double latitude, Double longitude, String supportedServices,
                                 String operatingHours, String networkType, String status) {
        this.id = id;
        this.agencyName = agencyName;
        this.contactNumber = contactNumber;
        this.address = address;
        this.pinCode = pinCode;
        this.city = city;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
        this.supportedServices = supportedServices;
        this.operatingHours = operatingHours;
        this.networkType = networkType;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAgencyName() { return agencyName; }
    public void setAgencyName(String agencyName) { this.agencyName = agencyName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

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

    public String getNetworkType() { return networkType; }
    public void setNetworkType(String networkType) { this.networkType = networkType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
