package com.lpgchain.service.notification;

/**
 * Encapsulates the dispatch outcome of an OTP handover notification.
 */
public record NotificationResult(
        boolean success,
        String status,
        String message,
        String referenceId
) {}
