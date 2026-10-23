package com.lpgchain.service.notification;

/**
 * DeliveryNotificationProvider — Interface for dispatching delivery handover OTPs
 * via registered channels (e.g. SMS via Twilio or development fallback).
 */
public interface DeliveryNotificationProvider {

    /**
     * Sends the delivery handover OTP to the customer's registered contact.
     *
     * @param recipientPhone customer's registered phone number
     * @param customerName   customer's name
     * @param deliveryId     unique delivery identifier
     * @param otp            plain-text OTP to transmit to customer
     * @return outcome of the notification dispatch
     */
    NotificationResult sendDeliveryHandoverOtp(String recipientPhone, String customerName, Long deliveryId, String otp);

    /**
     * Returns the provider identifier name.
     */
    String getProviderName();

    /**
     * Indicates whether this is a real external provider (e.g. Twilio)
     * or a development-only simulated test flow.
     */
    boolean isRealProvider();
}
