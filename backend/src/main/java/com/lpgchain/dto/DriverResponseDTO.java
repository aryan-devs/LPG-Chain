package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detailed representation of a delivery driver")
public class DriverResponseDTO {

    @Schema(description = "Unique driver identifier", example = "10")
    private Long id;

    @Schema(description = "Full name of the driver", example = "Rajesh Kumar")
    private String name;

    @Schema(description = "Contact phone number", example = "+919876543210")
    private String phone;

    @Schema(description = "Driving license number", example = "DL-0420110012345")
    private String licenseNumber;

    @Schema(description = "Current operational status", example = "AVAILABLE")
    private String status;

    @Schema(description = "Distributor organization ID", example = "1")
    private Long distributorId;

    @Schema(description = "Distributor agency name", example = "City Gas Agency")
    private String distributorName;

    public DriverResponseDTO() {
    }

    public DriverResponseDTO(Long id, String name, String phone, String licenseNumber,
                             String status, Long distributorId, String distributorName) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
        this.status = status;
        this.distributorId = distributorId;
        this.distributorName = distributorName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
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
}
