package com.lpgchain.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "ThisIsASecretKeyForTestingJwtTokenProviderMustBeAtLeast256BitsLong!";
    private static final long TEST_EXPIRATION_MS = 3600000; // 1 hour

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, TEST_EXPIRATION_MS);
    }

    // =========================================================================
    // TEST 1: Valid token generation
    // =========================================================================
    @Test
    @DisplayName("JWT 1: Generates non-null, non-blank compact token")
    void shouldGenerateValidToken() {
        String token = jwtTokenProvider.generateToken(1L, "user@example.com");

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(token.chars().filter(ch -> ch == '.').count() == 2);
    }

    // =========================================================================
    // TEST 2: Token contains expected subject / email
    // =========================================================================
    @Test
    @DisplayName("JWT 2: Extracts expected subject (email) from valid token")
    void shouldExtractExpectedSubjectFromToken() {
        String token = jwtTokenProvider.generateToken(42L, "operator@lpgchain.demo");

        String email = jwtTokenProvider.getEmailFromToken(token);
        assertEquals("operator@lpgchain.demo", email);
    }

    // =========================================================================
    // TEST 3 & 4: Token validation succeeds for valid token
    // =========================================================================
    @Test
    @DisplayName("JWT 3 & 4: Valid token passes validation")
    void shouldValidateValidTokenSuccessfully() {
        String token = jwtTokenProvider.generateToken(10L, "test@example.com");

        boolean isValid = jwtTokenProvider.validateToken(token);
        assertTrue(isValid);
    }

    // =========================================================================
    // TEST 5: Invalid / tampered token
    // =========================================================================
    @Test
    @DisplayName("JWT 5: Tampered token with altered signature or payload fails validation")
    void shouldRejectTamperedToken() {
        String token = jwtTokenProvider.generateToken(10L, "test@example.com");

        // Tamper with payload characters
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        boolean isValid = jwtTokenProvider.validateToken(tamperedToken);
        assertFalse(isValid);
    }

    // =========================================================================
    // TEST 6: Malformed token
    // =========================================================================
    @Test
    @DisplayName("JWT 6: Malformed tokens fail validation safely without throwing exceptions")
    void shouldRejectMalformedTokenSafely() {
        assertFalse(jwtTokenProvider.validateToken("not.a.valid.jwt"));
        assertFalse(jwtTokenProvider.validateToken("gibberish"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    // =========================================================================
    // TEST 7: Expired token
    // =========================================================================
    @Test
    @DisplayName("JWT 7: Expired token fails validation")
    void shouldRejectExpiredToken() {
        // Create a provider with negative expiration time (expired in the past)
        JwtTokenProvider expiredProvider = new JwtTokenProvider(TEST_SECRET, -1000L);
        String expiredToken = expiredProvider.generateToken(1L, "expired@example.com");

        boolean isValid = jwtTokenProvider.validateToken(expiredToken);
        assertFalse(isValid);
    }

    // =========================================================================
    // TEST 8: Active role claim
    // =========================================================================
    @Test
    @DisplayName("JWT 8: Active role claim is embedded and extracted correctly, returns null when omitted")
    void shouldHandleActiveRoleClaimCorrectly() {
        // Token without active role
        String unscopedToken = jwtTokenProvider.generateToken(1L, "user@example.com");
        assertNull(jwtTokenProvider.getRoleFromToken(unscopedToken));

        // Token with active role CUSTOMER
        String customerToken = jwtTokenProvider.generateToken(1L, "user@example.com", "CUSTOMER");
        assertEquals("CUSTOMER", jwtTokenProvider.getRoleFromToken(customerToken));

        // Token with active role DISTRIBUTOR
        String distributorToken = jwtTokenProvider.generateToken(1L, "user@example.com", "DISTRIBUTOR");
        assertEquals("DISTRIBUTOR", jwtTokenProvider.getRoleFromToken(distributorToken));
    }

    // =========================================================================
    // TEST 9: Distributor ID organization claim
    // =========================================================================
    @Test
    @DisplayName("JWT 9: Distributor organization ID claim is embedded and extracted correctly")
    void shouldHandleDistributorIdClaimCorrectly() {
        // Token without distributorId
        String noDistToken = jwtTokenProvider.generateToken(1L, "user@example.com", "DISTRIBUTOR");
        assertNull(jwtTokenProvider.getDistributorIdFromToken(noDistToken));

        // Token with distributorId
        String distToken = jwtTokenProvider.generateToken(1L, "user@example.com", "DISTRIBUTOR", 105L);
        assertEquals(105L, jwtTokenProvider.getDistributorIdFromToken(distToken));
    }

    // =========================================================================
    // TEST 10: Tokens signed with different secret fail validation
    // =========================================================================
    @Test
    @DisplayName("JWT 10: Token signed with foreign secret fails validation")
    void shouldRejectTokenSignedWithDifferentSecret() {
        String foreignSecret = "AnotherSecretKeyDifferentFromOriginalSecretMustBeAtLeast256BitsLong!";
        JwtTokenProvider foreignProvider = new JwtTokenProvider(foreignSecret, TEST_EXPIRATION_MS);

        String foreignToken = foreignProvider.generateToken(1L, "user@example.com");

        assertFalse(jwtTokenProvider.validateToken(foreignToken));
    }
}
