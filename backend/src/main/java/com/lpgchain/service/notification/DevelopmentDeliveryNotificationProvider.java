package com.lpgchain.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * DevelopmentDeliveryNotificationProvider — Explicitly labelled development-only test flow
 * active when no real notification provider is configured.
 * Never claims an SMS was sent when it was not.
 */
@Component
public class DevelopmentDeliveryNotificationProvider implements DeliveryNotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentDeliveryNotificationProvider.class);

    @Override
    public NotificationResult sendDeliveryHandoverOtp(String recipientPhone, String customerName, Long deliveryId, String otp) {
        String maskedRecipient = maskPhone(recipientPhone);
        log.info("[DEV-TEST-MODE] Delivery handover OTP requested for delivery #{}. Recipient: {}. "
                + "No real SMS provider is configured. Simulating delivery test flow.", deliveryId, maskedRecipient);

        return new NotificationResult(
                true,
                "DEV_TEST_MODE_ACTIVE",
                "Development mode: Notification provider not configured. No real SMS sent.",
                "DEV-SIM-" + deliveryId
        );
    }

    @Override
    public String getProviderName() {
        return "DEVELOPMENT_TEST_FLOW";
    }

    @Override
    public boolean isRealProvider() {
        return false;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "Unregistered Contact";
        }
        String clean = phone.trim();
        if (clean.length() <= 4) {
            return "****";
        }
        return clean.substring(0, Math.min(3, clean.length() - 4)) + "****" + clean.substring(clean.length() - 4);
    }
}
