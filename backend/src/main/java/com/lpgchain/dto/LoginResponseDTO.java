package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * LoginResponseDTO — Returned to client upon successful authentication or role selection.
 *
 * Contains user identifier, name, email, available roles, active role, JWT token,
 * roleSelectionRequired indicator, and status message.
 * Excludes passwords, hashes, and internal secrets.
 */
@Data
@Schema(description = "Authentication response containing JWT token, user details, and active role context")
public class LoginResponseDTO {

    @Schema(description = "Unique user identifier", example = "1")
    private Long userId;

    @Schema(description = "Full name of the user", example = "Integration User")
    private String name;

    @Schema(description = "Registered email address", example = "user@lpgchain.example")
    private String email;

    @Schema(description = "List of roles assigned to the user in the database", example = "[\"CUSTOMER\", \"DISTRIBUTOR\"]")
    private List<String> roles;

    @Schema(description = "Indicates whether the user must select an active role before accessing role-scoped endpoints", example = "false")
    private boolean roleSelectionRequired;

    @Schema(description = "Current active role in the issued JWT", example = "CUSTOMER")
    private String activeRole;

    @Schema(description = "Issued JWT Bearer token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Response status message", example = "Login successful")
    private String message;

    @Schema(description = "Active distributor organization ID context (if distributor role active)", example = "10")
    private Long activeRoleDistributorId;

    @Schema(description = "Active distributor organization ID context", example = "10")
    private Long activeDistributorId;

    @Schema(description = "Indicates whether the user must select an active distributor organization", example = "false")
    private boolean distributorSelectionRequired;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(Long userId, String name, String email, String token, String message) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.token = token;
        this.message = message;
    }

    public LoginResponseDTO(Long userId, String name, String email, List<String> roles,
                            boolean roleSelectionRequired, String activeRole, String token, String message) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.roles = roles;
        this.roleSelectionRequired = roleSelectionRequired;
        this.activeRole = activeRole;
        this.token = token;
        this.message = message;
    }

    public LoginResponseDTO(Long userId, String name, String email, List<String> roles,
                            boolean roleSelectionRequired, String activeRole, Long activeDistributorId,
                            boolean distributorSelectionRequired, String token, String message) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.roles = roles;
        this.roleSelectionRequired = roleSelectionRequired;
        this.activeRole = activeRole;
        this.activeDistributorId = activeDistributorId;
        this.activeRoleDistributorId = activeDistributorId;
        this.distributorSelectionRequired = distributorSelectionRequired;
        this.token = token;
        this.message = message;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public boolean isRoleSelectionRequired() { return roleSelectionRequired; }
    public void setRoleSelectionRequired(boolean roleSelectionRequired) { this.roleSelectionRequired = roleSelectionRequired; }

    public String getActiveRole() { return activeRole; }
    public void setActiveRole(String activeRole) { this.activeRole = activeRole; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getActiveDistributorId() { return activeDistributorId; }
    public void setActiveDistributorId(Long activeDistributorId) {
        this.activeDistributorId = activeDistributorId;
        this.activeRoleDistributorId = activeDistributorId;
    }

    public Long getActiveRoleDistributorId() { return activeRoleDistributorId; }
    public void setActiveRoleDistributorId(Long activeRoleDistributorId) {
        this.activeRoleDistributorId = activeRoleDistributorId;
        this.activeDistributorId = activeRoleDistributorId;
    }

    public boolean isDistributorSelectionRequired() { return distributorSelectionRequired; }
    public void setDistributorSelectionRequired(boolean distributorSelectionRequired) { this.distributorSelectionRequired = distributorSelectionRequired; }
}
