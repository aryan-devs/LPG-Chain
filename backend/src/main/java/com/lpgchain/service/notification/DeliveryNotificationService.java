package com.lpgchain.service.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * DeliveryNotificationService — Orchestrates OTP dispatch to customers,
 * picking the real configured provider (e.g. Twilio) when credentials exist,
 * or cleanly falling back to the explicitly labelled development test flow.
 */
@Service
public class DeliveryNotificationService {

    private final TwilioDeliveryNotificationProvider twilioProvider;
    private final DevelopmentDeliveryNotificationProvider devProvider;

    @Value("${lpgchain.notification.provider:dev}")
    private String preferredProvider;

    public DeliveryNotificationService(TwilioDeliveryNotificationProvider twilioProvider,
                                       DevelopmentDeliveryNotificationProvider devProvider) {
        this.twilioProvider = twilioProvider;
        this.devProvider = devProvider;
    }

    public DeliveryNotificationProvider getActiveProvider() {
        if ("twilio".equalsIgnoreCase(preferredProvider) && twilioProvider.isConfigured()) {
            return twilioProvider;
        }
        return devProvider;
    }

    public NotificationResult sendHandoverOtp(String recipientPhone, String customerName, Long deliveryId, String otp) {
        DeliveryNotificationProvider activeProvider = getActiveProvider();
        return activeProvider.sendDeliveryHandoverOtp(recipientPhone, customerName, deliveryId, otp);
    }
}
