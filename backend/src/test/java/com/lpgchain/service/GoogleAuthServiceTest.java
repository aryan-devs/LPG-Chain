package com.lpgchain.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    private static final String CLIENT_ID = "941879948100-r9vsn2v5uljml7neb3ncnvgcqutncu61.apps.googleusercontent.com";
    private static final String OTHER_CLIENT_ID = "wrong-client-id.apps.googleusercontent.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;
    private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        objectMapper = new ObjectMapper();

        googleAuthService = new GoogleAuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                jwtTokenProvider,
                restTemplate,
                objectMapper,
                CLIENT_ID
        );
    }

    private String buildGooglePayload(String sub, String email, String name, String aud, String iss, long exp, boolean emailVerified) {
        return String.format("{\n" +
                "  \"sub\": \"%s\",\n" +
                "  \"email\": \"%s\",\n" +
                "  \"name\": \"%s\",\n" +
                "  \"aud\": \"%s\",\n" +
                "  \"iss\": \"%s\",\n" +
                "  \"exp\": %d,\n" +
                "  \"email_verified\": \"%s\"\n" +
                "}", sub, email, name, aud, iss, exp, emailVerified);
    }

    @Test
    @DisplayName("GOOGLE AUTH: Successfully authenticates valid Google ID token for new user with CUSTOMER role")
    void shouldAuthenticateValidTokenForNewCustomer() {
        String token = "valid.google.idtoken";
        long futureExp = Instant.now().getEpochSecond() + 3600;
        String googleResponse = buildGooglePayload("google-sub-123", "alice@example.com", "Alice Tester",
                CLIENT_ID, "https://accounts.google.com", futureExp, true);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=" + token))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(googleResponse, MediaType.APPLICATION_JSON));

        when(userRepository.findByGoogleSub("google-sub-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setId(101L);
        savedUser.setEmail("alice@example.com");
        savedUser.setName("Alice Tester");
        savedUser.setGoogleSub("google-sub-123");

        Role customerRole = new Role(1L, "CUSTOMER");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(userRoleRepository.findByUser(savedUser)).thenReturn(List.of(new UserRole(1L, savedUser, customerRole)));
        when(jwtTokenProvider.generateToken(101L, "alice@example.com", "CUSTOMER", null)).thenReturn("jwt.session.token");

        LoginResponseDTO response = googleAuthService.authenticateWithGoogle(new GoogleAuthRequestDTO(token));

        assertNotNull(response);
        assertEquals("alice@example.com", response.getEmail());
        assertEquals("CUSTOMER", response.getActiveRole());
        assertEquals("jwt.session.token", response.getToken());
        mockServer.verify();
    }

    @Test
    @DisplayName("GOOGLE AUTH: Successfully links verified Google account to existing user by email")
    void shouldLinkGoogleAccountToExistingUser() {
        String token = "valid.google.idtoken";
        long futureExp = Instant.now().getEpochSecond() + 3600;
        String googleResponse = buildGooglePayload("google-sub-456", "bob@example.com", "Bob Existing",
                CLIENT_ID, "accounts.google.com", futureExp, true);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=" + token))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(googleResponse, MediaType.APPLICATION_JSON));

        User existingUser = new User();
        existingUser.setId(202L);
        existingUser.setEmail("bob@example.com");
        existingUser.setName("Bob Existing");

        Role customerRole = new Role(1L, "CUSTOMER");
        when(userRepository.findByGoogleSub("google-sub-456")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userRoleRepository.findByUser(existingUser)).thenReturn(List.of(new UserRole(2L, existingUser, customerRole)));
        when(jwtTokenProvider.generateToken(202L, "bob@example.com", "CUSTOMER", null)).thenReturn("jwt.bob.token");

        LoginResponseDTO response = googleAuthService.authenticateWithGoogle(new GoogleAuthRequestDTO(token));

        assertNotNull(response);
        assertEquals("bob@example.com", response.getEmail());
        assertEquals("google-sub-456", existingUser.getGoogleSub());
        mockServer.verify();
    }

    @Test
    @DisplayName("GOOGLE AUTH: Invalid token causes Google 400 Bad Request and throws IllegalArgumentException")
    void shouldThrowWhenGoogleRejectsInvalidToken() {
        String token = "invalid.dummy.token";

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=" + token))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_token\",\"error_description\":\"Invalid Value\"}"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                googleAuthService.authenticateWithGoogle(new GoogleAuthRequestDTO(token)));

        assertEquals("Invalid Google ID token", ex.getMessage());
        mockServer.verify();
    }

    @Test
    @DisplayName("GOOGLE AUTH: Audience mismatch throws SecurityException")
    void shouldThrowWhenAudienceDoesNotMatch() {
        String token = "foreign.client.idtoken";
        long futureExp = Instant.now().getEpochSecond() + 3600;
        String googleResponse = buildGooglePayload("google-sub-789", "charlie@example.com", "Charlie Foreign",
                OTHER_CLIENT_ID, "accounts.google.com", futureExp, true);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=" + token))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(googleResponse, MediaType.APPLICATION_JSON));

        SecurityException ex = assertThrows(SecurityException.class, () ->
                googleAuthService.authenticateWithGoogle(new GoogleAuthRequestDTO(token)));

        assertEquals("Google token audience mismatch", ex.getMessage());
        mockServer.verify();
    }

    @Test
    @DisplayName("GOOGLE AUTH: Expired token throws IllegalArgumentException")
    void shouldThrowWhenTokenIsExpired() {
        String token = "expired.google.idtoken";
        long pastExp = Instant.now().getEpochSecond() - 3600; // 1 hour ago
        String googleResponse = buildGooglePayload("google-sub-100", "david@example.com", "David Expired",
                CLIENT_ID, "accounts.google.com", pastExp, true);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=" + token))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(googleResponse, MediaType.APPLICATION_JSON));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                googleAuthService.authenticateWithGoogle(new GoogleAuthRequestDTO(token)));

        assertEquals("Google token has expired", ex.getMessage());
        mockServer.verify();
    }

    @Test
    @DisplayName("GOOGLE AUTH: Missing client ID configuration throws IllegalStateException")
    void shouldThrowWhenGoogleClientIdIsNotConfigured() {
        GoogleAuthService unconfiguredService = new GoogleAuthService(
                userRepository,
                roleRepository,
                userRoleRepository,
                jwtTokenProvider,
                restTemplate,
                objectMapper,
                "" // unconfigured empty client ID
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                unconfiguredService.authenticateWithGoogle(new GoogleAuthRequestDTO("any.token")));

        assertEquals("Google Sign-In is not configured on the server", ex.getMessage());
    }

    @Test
    @DisplayName("GOOGLE AUTH: Unverified email throws IllegalArgumentException")
    void shouldThrowWhenGoogleEmailIsNotVerified() {
        String token = "unverified.email.idtoken";
        long futureExp = Instant.now().getEpochSecond() + 3600;
        String googleResponse = buildGooglePayload("google-sub-unverified", "unverified@example.com", "Unverified User",
                CLIENT_ID, "accounts.google.com", futureExp, false);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=" + token))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(googleResponse, MediaType.APPLICATION_JSON));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                googleAuthService.authenticateWithGoogle(new GoogleAuthRequestDTO(token)));

        assertEquals("Google email address is not verified", ex.getMessage());
        mockServer.verify();
    }
}
