package com.lpgchain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response returned when delivery doorstep handover is initiated")
public class DeliveryHandoverResponseDTO {

    @Schema(description = "Delivery unique identifier", example = "501")
    private Long deliveryId;

    @Schema(description = "Current delivery status", example = "OUT_FOR_DELIVERY")
    private String status;

    @Schema(description = "Handover lifecycle stage", example = "OTP_DISPATCHED")
    private String handoverStage;

    @Schema(description = "Masked recipient contact number", example = "+91 ******3210")
    private String recipientPhoneMasked;

    @Schema(description = "Timestamp when OTP expires")
    private LocalDateTime otpExpiresAt;

    @Schema(description = "Number of remaining verification attempts allowed", example = "3")
    private Integer remainingAttempts;

    @Schema(description = "Notification dispatch status", example = "DEV_TEST_MODE_ACTIVE")
    private String notificationStatus;

    @Schema(description = "Human-readable notification status message")
    private String notificationMessage;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Development-only test OTP (omitted in production or when a real SMS provider is configured)")
    private String devTestOtp;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Explicit development warning note")
    private String devTestNotice;

    public DeliveryHandoverResponseDTO() {
    }

    public DeliveryHandoverResponseDTO(Long deliveryId, String status, String handoverStage,
                                       String recipientPhoneMasked, LocalDateTime otpExpiresAt,
                                       Integer remainingAttempts, String notificationStatus,
                                       String notificationMessage) {
        this.deliveryId = deliveryId;
        this.status = status;
        this.handoverStage = handoverStage;
        this.recipientPhoneMasked = recipientPhoneMasked;
        this.otpExpiresAt = otpExpiresAt;
        this.remainingAttempts = remainingAttempts;
        this.notificationStatus = notificationStatus;
        this.notificationMessage = notificationMessage;
    }

    public Long getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId) {
        this.deliveryId = deliveryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHandoverStage() {
        return handoverStage;
    }

    public void setHandoverStage(String handoverStage) {
        this.handoverStage = handoverStage;
    }

    public String getRecipientPhoneMasked() {
        return recipientPhoneMasked;
    }

    public void setRecipientPhoneMasked(String recipientPhoneMasked) {
        this.recipientPhoneMasked = recipientPhoneMasked;
    }

    public LocalDateTime getOtpExpiresAt() {
        return otpExpiresAt;
    }

    public void setOtpExpiresAt(LocalDateTime otpExpiresAt) {
        this.otpExpiresAt = otpExpiresAt;
    }

    public Integer getRemainingAttempts() {
        return remainingAttempts;
    }

    public void setRemainingAttempts(Integer remainingAttempts) {
        this.remainingAttempts = remainingAttempts;
    }

    public String getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(String notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

    public void setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
    }

    public String getDevTestOtp() {
        return devTestOtp;
    }

    public void setDevTestOtp(String devTestOtp) {
        this.devTestOtp = devTestOtp;
    }

    public String getDevTestNotice() {
        return devTestNotice;
    }

    public void setDevTestNotice(String devTestNotice) {
        this.devTestNotice = devTestNotice;
    }
}
