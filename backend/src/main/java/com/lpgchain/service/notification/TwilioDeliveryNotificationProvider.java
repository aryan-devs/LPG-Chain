package com.lpgchain.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * TwilioDeliveryNotificationProvider — Real SMS dispatch integration via Twilio REST API.
 * Active when valid Twilio credentials are configured in application properties or environment variables.
 */
@Component
public class TwilioDeliveryNotificationProvider implements DeliveryNotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioDeliveryNotificationProvider.class);

    @Value("${lpgchain.notification.twilio.account-sid:}")
    private String accountSid;

    @Value("${lpgchain.notification.twilio.auth-token:}")
    private String authToken;

    @Value("${lpgchain.notification.twilio.from-number:}")
    private String fromNumber;

    private final HttpClient httpClient;

    public TwilioDeliveryNotificationProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public TwilioDeliveryNotificationProvider(String accountSid, String authToken, String fromNumber, HttpClient httpClient) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public NotificationResult sendDeliveryHandoverOtp(String recipientPhone, String customerName, Long deliveryId, String otp) {
        if (!isConfigured()) {
            return new NotificationResult(
                    false,
                    "PROVIDER_UNCONFIGURED",
                    "Twilio SMS provider is not fully configured (missing accountSid, authToken, or fromNumber)",
                    null
            );
        }

        if (!StringUtils.hasText(recipientPhone)) {
            return new NotificationResult(
                    false,
                    "MISSING_RECIPIENT_CONTACT",
                    "Customer registered phone number is missing",
                    null
            );
        }

        String maskedRecipient = maskPhone(recipientPhone);
        log.info("Sending delivery handover SMS OTP via Twilio for delivery #{} to {}", deliveryId, maskedRecipient);

        try {
            String messageBody = String.format(
                    "Your LPG-Chain delivery handover OTP is %s. Share this OTP with the delivery driver only after inspecting the cylinder. Valid for 10 minutes.",
                    otp
            );

            String formPayload = "To=" + URLEncoder.encode(recipientPhone, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(fromNumber, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(messageBody, StandardCharsets.UTF_8);

            String authHeader = "Basic " + Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("SMS OTP successfully dispatched via Twilio for delivery #{}. Status code: {}", deliveryId, response.statusCode());
                return new NotificationResult(
                        true,
                        "SMS_SENT",
                        "Delivery handover OTP sent via SMS to customer's registered phone",
                        "TW-" + deliveryId + "-" + System.currentTimeMillis()
                );
            } else {
                log.error("Twilio SMS dispatch returned HTTP {} for delivery #{}: {}", response.statusCode(), deliveryId, response.body());
                return new NotificationResult(
                        false,
                        "SMS_DISPATCH_FAILED",
                        "SMS provider returned error code: " + response.statusCode(),
                        null
                );
            }
        } catch (Exception e) {
            log.error("Exception during Twilio SMS dispatch for delivery #{}: {}", deliveryId, e.getMessage());
            return new NotificationResult(
                    false,
                    "SMS_DISPATCH_EXCEPTION",
                    "Failed to dispatch SMS notification: " + e.getMessage(),
                    null
            );
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(accountSid)
                && StringUtils.hasText(authToken)
                && StringUtils.hasText(fromNumber);
    }

    @Override
    public String getProviderName() {
        return "TWILIO";
    }

    @Override
    public boolean isRealProvider() {
        return isConfigured();
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
