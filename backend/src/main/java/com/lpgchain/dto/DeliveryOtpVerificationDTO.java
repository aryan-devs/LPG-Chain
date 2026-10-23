package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Payload submitted by driver to verify delivery handover OTP")
public class DeliveryOtpVerificationDTO {

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{4,8}$", message = "OTP must be a numeric code")
    @Schema(description = "One-Time Password provided by customer upon inspection", example = "592814")
    private String otp;

    public DeliveryOtpVerificationDTO() {
    }

    public DeliveryOtpVerificationDTO(String otp) {
        this.otp = otp;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
