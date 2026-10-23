package com.lpgchain.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.GoogleAuthRequestDTO;
import com.lpgchain.dto.LoginResponseDTO;
import com.lpgchain.service.GoogleAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GoogleAuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoogleAuthService googleAuthService;

    @Test
    @DisplayName("GOOGLE AUTH CONTROLLER: POST /api/auth/google with valid token returns 200 OK + JWT")
    void shouldReturn200WithValidGoogleToken() throws Exception {
        LoginResponseDTO mockResponse = new LoginResponseDTO(
                10L,
                "Google User",
                "googleuser@example.com",
                List.of("CUSTOMER"),
                false,
                "CUSTOMER",
                null,
                false,
                "valid.mock.jwt.token",
                "Google authentication successful"
        );

        when(googleAuthService.authenticateWithGoogle(any(GoogleAuthRequestDTO.class))).thenReturn(mockResponse);

        GoogleAuthRequestDTO request = new GoogleAuthRequestDTO("valid.google.id.token");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("googleuser@example.com"))
                .andExpect(jsonPath("$.activeRole").value("CUSTOMER"))
                .andExpect(jsonPath("$.token").value("valid.mock.jwt.token"));
    }

    @Test
    @DisplayName("GOOGLE AUTH CONTROLLER: POST /api/auth/google with invalid token returns 400 Bad Request")
    void shouldReturn400WhenGoogleTokenIsInvalid() throws Exception {
        when(googleAuthService.authenticateWithGoogle(any(GoogleAuthRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Invalid Google ID token"));

        GoogleAuthRequestDTO request = new GoogleAuthRequestDTO("invalid.dummy.token");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid Google ID token"));
    }

    @Test
    @DisplayName("GOOGLE AUTH CONTROLLER: POST /api/auth/google with wrong audience returns 403 Forbidden")
    void shouldReturn403WhenAudienceMismatch() throws Exception {
        when(googleAuthService.authenticateWithGoogle(any(GoogleAuthRequestDTO.class)))
                .thenThrow(new SecurityException("Google token audience mismatch"));

        GoogleAuthRequestDTO request = new GoogleAuthRequestDTO("foreign.audience.token");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Security violation: Google token audience mismatch"));
    }

    @Test
    @DisplayName("GOOGLE AUTH CONTROLLER: POST /api/auth/google when unconfigured returns 400 Bad Request")
    void shouldReturn400WhenServerGoogleAuthUnconfigured() throws Exception {
        when(googleAuthService.authenticateWithGoogle(any(GoogleAuthRequestDTO.class)))
                .thenThrow(new IllegalStateException("Google Sign-In is not configured on the server"));

        GoogleAuthRequestDTO request = new GoogleAuthRequestDTO("any.token");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Google Sign-In is not configured on the server"));
    }

    @Test
    @DisplayName("GOOGLE AUTH CONTROLLER: POST /api/auth/google with expired token returns 400 Bad Request")
    void shouldReturn400WhenTokenIsExpired() throws Exception {
        when(googleAuthService.authenticateWithGoogle(any(GoogleAuthRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Google token has expired"));

        GoogleAuthRequestDTO request = new GoogleAuthRequestDTO("expired.token");

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Google token has expired"));
    }
}
