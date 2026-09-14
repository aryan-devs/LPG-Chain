package com.lpgchain.service;

import com.lpgchain.config.EmailPolicyProperties;
import com.lpgchain.exception.InvalidEmailPolicyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * EmailPolicyService — Enforces email format, domain validation, and role-specific
 * official email policies across LPG-Chain.
 *
 * Rules:
 *  - CUSTOMER: Permissive policy accepting personal and custom email addresses.
 *  - DISTRIBUTOR: Requires an official approved distributor organization domain.
 *  - ADMIN: Requires an approved official LPG-Chain administrative email domain.
 *
 * Security Note:
 *  Email policy validation is purely a business identity check and NEVER assigns
 *  roles or memberships on its own. The database remains the ultimate source of truth.
 */
@Service
public class EmailPolicyService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    private final EmailPolicyProperties emailPolicyProperties;

    public EmailPolicyService(EmailPolicyProperties emailPolicyProperties) {
        this.emailPolicyProperties = emailPolicyProperties;
    }

    /**
     * Syntactically validates an email address.
     */
    public boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        String trimmed = email.trim();
        return EMAIL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Extracts the lowercase domain part of an email address after the '@'.
     * Returns null if email is malformed.
     */
    public String extractDomain(String email) {
        if (!isValidEmail(email)) {
            return null;
        }
        int atIndex = email.lastIndexOf('@');
        if (atIndex == -1 || atIndex == email.length() - 1) {
            return null;
        }
        return email.substring(atIndex + 1).trim().toLowerCase();
    }

    /**
     * Checks if the email belongs to an approved distributor organization domain.
     * Uses strict exact domain matching (case-insensitive) to prevent domain spoofing.
     */
    public boolean isDistributorOfficialEmail(String email) {
        String domain = extractDomain(email);
        if (domain == null) {
            return false;
        }
        return emailPolicyProperties.getDistributorDomains().contains(domain);
    }

    /**
     * Checks if the email belongs to an approved admin domain.
     * Uses strict exact domain matching (case-insensitive) to prevent domain spoofing.
     */
    public boolean isAdminOfficialEmail(String email) {
        String domain = extractDomain(email);
        if (domain == null) {
            return false;
        }
        return emailPolicyProperties.getAdminDomains().contains(domain);
    }

    /**
     * Validates customer email format. Customers may use personal domains (gmail, outlook, etc.).
     */
    public boolean isCustomerEmailValid(String email) {
        return isValidEmail(email);
    }

    /**
     * Validates customer email policy. Throws InvalidEmailPolicyException if format is invalid.
     */
    public void validateCustomerEmail(String email) {
        if (!isCustomerEmailValid(email)) {
            throw new InvalidEmailPolicyException("Customer email address format is invalid: " + email);
        }
    }

    /**
     * Validates distributor email policy. Throws InvalidEmailPolicyException if email
     * is not from an approved distributor domain.
     */
    public void validateDistributorEmail(String email) {
        if (!isValidEmail(email)) {
            throw new InvalidEmailPolicyException("Distributor email address format is invalid: " + email);
        }
        if (!isDistributorOfficialEmail(email)) {
            throw new InvalidEmailPolicyException("Distributor accounts must use an approved official organization email address.");
        }
    }

    /**
     * Validates admin email policy. Throws InvalidEmailPolicyException if email
     * is not from an approved admin domain.
     */
    public void validateAdminEmail(String email) {
        if (!isValidEmail(email)) {
            throw new InvalidEmailPolicyException("Admin email address format is invalid: " + email);
        }
        if (!isAdminOfficialEmail(email)) {
            throw new InvalidEmailPolicyException("Admin accounts must use an approved official LPG-Chain admin email address.");
        }
    }

    /**
     * Validates an email address against the policy for the specified role.
     */
    public void validateEmailForRole(String email, String role) {
        if (role == null || role.isBlank()) {
            validateCustomerEmail(email);
            return;
        }

        String normalized = role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        switch (normalized) {
            case "DISTRIBUTOR" -> validateDistributorEmail(email);
            case "ADMIN"       -> validateAdminEmail(email);
            case "CUSTOMER"    -> validateCustomerEmail(email);
            default            -> validateCustomerEmail(email);
        }
    }
}
