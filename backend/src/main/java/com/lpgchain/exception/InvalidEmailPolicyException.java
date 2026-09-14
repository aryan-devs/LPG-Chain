package com.lpgchain.exception;

/**
 * InvalidEmailPolicyException — Thrown when an email address violates
 * role-specific email domain policy or formatting requirements.
 */
public class InvalidEmailPolicyException extends RuntimeException {

    public InvalidEmailPolicyException(String message) {
        super(message);
    }
}
