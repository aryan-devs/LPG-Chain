package com.lpgchain.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JwtTokenProvider — responsible for generating and validating JWT tokens.
 *
 * The secret key and expiration are read from configuration (never hardcoded).
 *
 * Claims embedded in the token:
 *  - sub  (subject)  : user's email address
 *  - userId          : user's database ID
 *  - iat  (issued at): timestamp of creation
 *  - exp  (expiry)   : timestamp after which the token is invalid
 *
 * The user's password is NEVER placed in the token.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long      jwtExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") long jwtExpiration) {

        // Derive an HMAC-SHA key from the configured secret string.
        // The secret must be at least 32 characters (256 bits) for HS256.
        this.signingKey    = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpiration = jwtExpiration;
    }

    /**
     * Generates a signed JWT for the authenticated user without an active role.
     *
     * @param userId  the user's database primary key
     * @param email   the user's email (used as JWT subject)
     * @return        compact, URL-safe JWT string
     */
    public String generateToken(Long userId, String email) {
        return generateToken(userId, email, null);
    }

    /**
     * Generates a signed JWT for the authenticated user with an active role claim.
     *
     * @param userId  the user's database primary key
     * @param email   the user's email (used as JWT subject)
     * @param role    the user's selected active role (e.g. "CUSTOMER", "DISTRIBUTOR", "ADMIN")
     * @return        compact, URL-safe JWT string
     */
    public String generateToken(Long userId, String email, String role) {
        return generateToken(userId, email, role, null);
    }

    /**
     * Generates a signed JWT for the authenticated user with an active role claim
     * and an optional distributorId organization context claim.
     *
     * @param userId         the user's database primary key
     * @param email          the user's email (used as JWT subject)
     * @param role           the user's selected active role (e.g. "CUSTOMER", "DISTRIBUTOR", "ADMIN")
     * @param distributorId  the selected active distributor organization ID (optional)
     * @return compact, URL-safe JWT string
     */
    public String generateToken(Long userId, String email, String role, Long distributorId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtExpiration);

        var builder = Jwts.builder()
                .subject(email)                   // standard "sub" claim
                .claim("userId", userId)           // custom claim
                .issuedAt(now)                     // standard "iat" claim
                .expiration(expiry)                // standard "exp" claim
                .signWith(signingKey);             // HMAC-SHA256 signature

        if (role != null && !role.isBlank()) {
            builder.claim("role", role);          // custom active-role claim
        }

        if (distributorId != null) {
            builder.claim("distributorId", distributorId); // custom active-distributor claim
        }

        return builder.compact();
    }

    /**
     * Validates whether the token is signed with our key, not expired, and well-formed.
     *
     * @param token compact JWT string
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token is expired, signature is invalid, or token is malformed
            return false;
        }
    }

    /**
     * Extracts the subject (email) from a valid JWT token.
     *
     * @param token compact JWT string
     * @return the user's email address
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * Extracts the active role claim from a valid JWT token.
     *
     * @param token compact JWT string
     * @return the active role string, or null if no active role is set
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("role", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Extracts the distributorId claim from a valid JWT token.
     *
     * @param token compact JWT string
     * @return the distributor ID, or null if no distributor context is set
     */
    public Long getDistributorIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Object val = claims.get("distributorId");
            if (val instanceof Number) {
                return ((Number) val).longValue();
            }
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}

