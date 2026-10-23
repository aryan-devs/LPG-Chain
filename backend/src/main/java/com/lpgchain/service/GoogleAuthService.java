package com.lpgchain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.GoogleAuthRequestDTO;
import com.lpgchain.dto.LoginResponseDTO;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.repository.RoleRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${google.oauth.client-id:${google.client-id:}}")
    private String configuredClientId;

    @org.springframework.beans.factory.annotation.Autowired
    public GoogleAuthService(UserRepository userRepository,
                             RoleRepository roleRepository,
                             UserRoleRepository userRoleRepository,
                             JwtTokenProvider jwtTokenProvider,
                             RestTemplateBuilder restTemplateBuilder,
                             ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
    }

    // Testing constructor allowing injected RestTemplate and custom clientId
    public GoogleAuthService(UserRepository userRepository,
                             RoleRepository roleRepository,
                             UserRoleRepository userRoleRepository,
                             JwtTokenProvider jwtTokenProvider,
                             RestTemplate restTemplate,
                             ObjectMapper objectMapper,
                             String configuredClientId) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.configuredClientId = configuredClientId;
    }

    /**
     * Authenticates a user using Google OAuth 2.0 / OpenID Connect ID token.
     * Validates signature, audience, issuer, expiry, and email verification.
     * Ensures new sign-ups are assigned ONLY the CUSTOMER role.
     */
    public LoginResponseDTO authenticateWithGoogle(GoogleAuthRequestDTO request) {
        if (request == null || request.getIdToken() == null || request.getIdToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Google ID token credential is required");
        }

        JsonNode payload = verifyGoogleIdToken(request.getIdToken().trim());
        String googleSub = payload.path("sub").asText();
        String email = payload.path("email").asText();
        String name = payload.path("name").asText(email);

        if (googleSub == null || googleSub.trim().isEmpty()) {
            throw new IllegalArgumentException("Google token missing 'sub' claim");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Google token missing 'email' claim");
        }

        // 1. Try to find user by stable googleSub
        Optional<User> userBySub = userRepository.findByGoogleSub(googleSub);
        User user;

        if (userBySub.isPresent()) {
            user = userBySub.get();
        } else {
            // 2. Check if user exists by verified email
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                user = userByEmail.get();
                // Secure account linking: bind the verified Google sub
                user.setGoogleSub(googleSub);
                userRepository.save(user);
            } else {
                // 3. New user registration: CUSTOMER role ONLY (principle of least privilege)
                user = new User();
                user.setName(name);
                user.setEmail(email);
                user.setGoogleSub(googleSub);
                user.setAddress("");
                user.setCity("");
                user.setState("");
                user.setPincode("");
                user.setConsumerNumber("LPG-" + System.currentTimeMillis() % 1000000);
                User savedUser = userRepository.save(user);

                // Assign default CUSTOMER role
                Role customerRole = roleRepository.findByName("CUSTOMER")
                        .orElseGet(() -> roleRepository.save(new Role("CUSTOMER")));
                userRoleRepository.save(new UserRole(savedUser, customerRole));

                user = savedUser;
            }
        }

        // Determine user's active role (Google registration never escalates privileges)
        List<UserRole> userRoles = userRoleRepository.findByUser(user);
        List<String> cleanRoles = userRoles.stream()
                .map(UserRole::getRole)
                .map(Role::getName)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .distinct()
                .toList();

        String activeRole = cleanRoles.contains("ADMIN") ? "ADMIN" :
                (cleanRoles.contains("DISTRIBUTOR") ? "DISTRIBUTOR" : "CUSTOMER");

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), activeRole, null);

        return new LoginResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                cleanRoles,
                false,
                activeRole,
                null,
                false,
                token,
                "Google authentication successful"
        );
    }

    private JsonNode verifyGoogleIdToken(String idToken) {
        if (configuredClientId == null || configuredClientId.trim().isEmpty()) {
            log.warn("Google Sign-In attempted but Google OAuth Client ID is not configured on the backend");
            throw new IllegalStateException("Google Sign-In is not configured on the server");
        }

        String verifyUrl = "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}";
        String responseBody;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(verifyUrl, String.class, idToken);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Google token verification returned non-2xx status: {}", response.getStatusCode());
                throw new IllegalArgumentException("Invalid Google ID token");
            }
            responseBody = response.getBody();
        } catch (HttpStatusCodeException ex) {
            log.warn("Google OAuth service rejected token: status={}", ex.getStatusCode());
            throw new IllegalArgumentException("Invalid Google ID token");
        } catch (ResourceAccessException ex) {
            log.error("Google OAuth token verification service unreachable: {}", ex.getMessage());
            throw new IllegalArgumentException("Google OAuth verification service is unreachable");
        } catch (Exception ex) {
            log.error("Google token verification request failed");
            throw new IllegalArgumentException("Invalid Google ID token");
        }

        try {
            JsonNode node = objectMapper.readTree(responseBody);

            // 1. Audience validation against configured Google Client ID
            String tokenAud = node.path("aud").asText();
            if (!configuredClientId.trim().equals(tokenAud)) {
                log.warn("Google token audience mismatch");
                throw new SecurityException("Google token audience mismatch");
            }

            // 2. Issuer validation (must be accounts.google.com or https://accounts.google.com)
            String iss = node.path("iss").asText();
            if (!"accounts.google.com".equals(iss) && !"https://accounts.google.com".equals(iss)) {
                log.warn("Invalid Google token issuer: {}", iss);
                throw new SecurityException("Invalid Google token issuer");
            }

            // 3. Expiration validation
            long exp = node.path("exp").asLong(0);
            long now = Instant.now().getEpochSecond();
            if (exp > 0 && exp < now) {
                log.warn("Google token expired: exp={}, now={}", exp, now);
                throw new IllegalArgumentException("Google token has expired");
            }

            // 4. Email verification claim validation
            String emailVerified = node.path("email_verified").asText();
            if (!"true".equalsIgnoreCase(emailVerified) && !node.path("email_verified").asBoolean(false)) {
                log.warn("Google account email is not verified");
                throw new IllegalArgumentException("Google email address is not verified");
            }

            return node;
        } catch (SecurityException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Google verification response");
            throw new IllegalArgumentException("Invalid Google ID token response");
        }
    }
}
