package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for creating or updating a delivery driver")
public class DriverRequestDTO {

    @NotBlank(message = "name is required")
    @Schema(description = "Full name of the driver", example = "Rajesh Kumar", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "phone is required")
    @Schema(description = "Contact phone number of the driver", example = "+919876543210", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank(message = "licenseNumber is required")
    @Schema(description = "Commercial driving license number", example = "DL-0420110012345", requiredMode = Schema.RequiredMode.REQUIRED)
    private String licenseNumber;

    @NotNull(message = "distributorId is required")
    @Schema(description = "Distributor ID to which this driver belongs", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long distributorId;

    @Schema(description = "Driver status (AVAILABLE, ASSIGNED, ON_DELIVERY, INACTIVE)", example = "AVAILABLE")
    private String status;

    public DriverRequestDTO() {
    }

    public DriverRequestDTO(String name, String phone, String licenseNumber, Long distributorId, String status) {
        this.name = name;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
        this.distributorId = distributorId;
        this.status = status;
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

    public Long getDistributorId() {
        return distributorId;
    }

    public void setDistributorId(Long distributorId) {
        this.distributorId = distributorId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
