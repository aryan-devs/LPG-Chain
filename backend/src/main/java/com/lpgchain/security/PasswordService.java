package com.lpgchain.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * PasswordService — Centralizes password hashing and verification using BCrypt + Pepper.
 *
 * Combines raw passwords with an application-level secret pepper before delegating
 * to BCryptPasswordEncoder. Also provides safe backward-compatibility and migration
 * for legacy unpeppered passwords.
 */
@Service
public class PasswordService {

    private final PasswordEncoder passwordEncoder;
    private final String pepper;

    public PasswordService(
            PasswordEncoder passwordEncoder,
            @Value("${password.pepper}") String pepper) {
        this.passwordEncoder = passwordEncoder;
        this.pepper = pepper;
    }

    /**
     * Combines the raw password with the application pepper and hashes it using BCrypt.
     *
     * @param rawPassword plain-text password
     * @return BCrypt hash of (rawPassword + pepper)
     */
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        return passwordEncoder.encode(rawPassword + pepper);
    }

    /**
     * Verifies whether the raw password matches the stored BCrypt hash.
     * Checks with the pepper first. If that fails, checks without pepper for
     * seamless backward compatibility with existing users.
     *
     * @param rawPassword plain-text password from login request
     * @param storedHash  BCrypt hash stored in the database
     * @return true if credentials match, false otherwise
     */
    public boolean matches(CharSequence rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        // 1. Primary check: with pepper
        if (passwordEncoder.matches(rawPassword + pepper, storedHash)) {
            return true;
        }
        // 2. Backward-compatibility check: without pepper (legacy users)
        return passwordEncoder.matches(rawPassword, storedHash);
    }

    /**
     * Identifies if a legacy password needs migration to peppered format.
     * Returns true only when the password matches without pepper but fails with pepper.
     *
     * @param rawPassword plain-text password from login request
     * @param storedHash  BCrypt hash stored in the database
     * @return true if legacy and needs upgrade, false otherwise
     */
    public boolean isLegacyUnpeppered(CharSequence rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        // If it matches with pepper, it is already peppered (not legacy)
        if (passwordEncoder.matches(rawPassword + pepper, storedHash)) {
            return false;
        }
        // If it matches without pepper, it is legacy and should be upgraded
        return passwordEncoder.matches(rawPassword, storedHash);
    }
}
