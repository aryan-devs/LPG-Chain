package com.lpgchain.security;

import com.lpgchain.dto.LoginResponseDTO;
import com.lpgchain.dto.UserResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordSecurityTest {

    private PasswordService passwordService;
    private final String testPepper = "test-external-pepper-value-from-config-12345";

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService(new BCryptPasswordEncoder(), testPepper);
    }

    // =========================================================================
    // TEST 1 & 2: Passwords are encrypted with BCrypt prefix, never stored raw
    // =========================================================================
    @Test
    @DisplayName("PASSWORD 1 & 2: Encoded passwords use BCrypt standard ($2a$) and never match raw text")
    void shouldEncodePasswordWithBCryptFormat() {
        String rawPassword = "SuperSecretPassword@123";
        String encoded = passwordService.encode(rawPassword);

        assertNotNull(encoded);
        assertNotEquals(rawPassword, encoded);
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"),
                "Encoded password must be a valid BCrypt hash starting with $2a$ or $2b$");
    }

    // =========================================================================
    // TEST 3: Salting randomness
    // =========================================================================
    @Test
    @DisplayName("PASSWORD 3: Same password hashed twice produces different hashes due to BCrypt salts")
    void shouldProduceDifferentHashesForSamePassword() {
        String rawPassword = "IdenticalPassword@123";
        String hash1 = passwordService.encode(rawPassword);
        String hash2 = passwordService.encode(rawPassword);

        assertNotEquals(hash1, hash2, "BCrypt must automatically generate random salt for each hash");
        assertTrue(passwordService.matches(rawPassword, hash1));
        assertTrue(passwordService.matches(rawPassword, hash2));
    }

    // =========================================================================
    // TEST 4 & 5: Matching with correct vs incorrect password
    // =========================================================================
    @Test
    @DisplayName("PASSWORD 4 & 5: Credentials match with correct password and fail with incorrect password")
    void shouldVerifyPasswordCorrectly() {
        String rawPassword = "CorrectPassword@123";
        String hash = passwordService.encode(rawPassword);

        assertTrue(passwordService.matches(rawPassword, hash));
        assertFalse(passwordService.matches("WrongPassword@123", hash));
        assertFalse(passwordService.matches("", hash));
        assertFalse(passwordService.matches(null, hash));
    }

    // =========================================================================
    // TEST 6: DTO security inspection (Reflection check: no password fields exposed)
    // =========================================================================
    @Test
    @DisplayName("PASSWORD 6: UserResponseDTO and LoginResponseDTO do not contain password or hash fields")
    void shouldNotExposePasswordFieldsInResponseDTOs() {
        Field[] userResponseFields = UserResponseDTO.class.getDeclaredFields();
        boolean hasPasswordInUserResponse = Arrays.stream(userResponseFields)
                .anyMatch(f -> f.getName().toLowerCase().contains("password")
                        || f.getName().toLowerCase().contains("hash")
                        || f.getName().toLowerCase().contains("pepper"));

        assertFalse(hasPasswordInUserResponse, "UserResponseDTO must never declare password, hash, or pepper fields");

        Field[] loginResponseFields = LoginResponseDTO.class.getDeclaredFields();
        boolean hasPasswordInLoginResponse = Arrays.stream(loginResponseFields)
                .anyMatch(f -> f.getName().toLowerCase().contains("password")
                        || f.getName().toLowerCase().contains("hash")
                        || f.getName().toLowerCase().contains("pepper"));

        assertFalse(hasPasswordInLoginResponse, "LoginResponseDTO must never declare password, hash, or pepper fields");
    }

    // =========================================================================
    // TEST 7: Backward compatibility with legacy unpeppered passwords
    // =========================================================================
    @Test
    @DisplayName("PASSWORD 7: Matches legacy unpeppered passwords for automatic seamless upgrade")
    void shouldMatchLegacyUnpepperedPassword() {
        BCryptPasswordEncoder rawBCrypt = new BCryptPasswordEncoder();
        String rawPassword = "LegacyPassword@123";
        // Hash created without application pepper
        String legacyHash = rawBCrypt.encode(rawPassword);

        // PasswordService matches() checks pepper first, then checks without pepper
        assertTrue(passwordService.matches(rawPassword, legacyHash),
                "PasswordService must verify legacy unpeppered hashes for backward compatibility");
        assertTrue(passwordService.isLegacyUnpeppered(rawPassword, legacyHash),
                "isLegacyUnpeppered must identify legacy hashes for upgrade");
    }
}
