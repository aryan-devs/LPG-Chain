package com.lpgchain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload for Google OAuth 2.0 / OpenID Connect token verification")
public class GoogleAuthRequestDTO {

    @NotBlank(message = "Google ID token credential is required")
    @JsonAlias({"credential", "id_token"})
    private String idToken;

    public GoogleAuthRequestDTO() {}

    public GoogleAuthRequestDTO(String idToken) {
        this.idToken = idToken;
    }

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }
}
