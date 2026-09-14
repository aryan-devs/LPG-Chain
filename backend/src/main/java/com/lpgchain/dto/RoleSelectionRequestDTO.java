package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * RoleSelectionRequestDTO — Payload for selecting an active role for multi-role users.
 */
@Data
@Schema(description = "Request payload for selecting an active role from user's assigned roles")
public class RoleSelectionRequestDTO {

    @NotBlank(message = "Role must not be blank")
    @Schema(description = "Role name to activate (e.g., CUSTOMER, DISTRIBUTOR, ADMIN)", example = "CUSTOMER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String role;

    public RoleSelectionRequestDTO() {
    }

    public RoleSelectionRequestDTO(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
