package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * DistributorSelectionResponseDTO — Response payload when a distributor organization is selected.
 */
@Data
@Schema(description = "Response payload after selecting an active distributor organization context")
public class DistributorSelectionResponseDTO {

    @Schema(description = "User ID", example = "2")
    private Long userId;

    @Schema(description = "Selected distributor organization ID", example = "1")
    private Long distributorId;

    @Schema(description = "Name of the distributor agency", example = "City Gas Agency")
    private String agencyName;

    @Schema(description = "User's role within this distributor agency", example = "OWNER")
    private String membershipRole;

    @Schema(description = "Membership status", example = "ACTIVE")
    private String status;

    @Schema(description = "New JWT token containing the active distributor organization context claim", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Response status message", example = "Distributor context selected successfully")
    private String message;

    public DistributorSelectionResponseDTO() {
    }

    public DistributorSelectionResponseDTO(Long userId, Long distributorId, String agencyName,
                                          String membershipRole, String status, String token, String message) {
        this.userId = userId;
        this.distributorId = distributorId;
        this.agencyName = agencyName;
        this.membershipRole = membershipRole;
        this.status = status;
        this.token = token;
        this.message = message;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getDistributorId() { return distributorId; }
    public void setDistributorId(Long distributorId) { this.distributorId = distributorId; }

    public String getAgencyName() { return agencyName; }
    public void setAgencyName(String agencyName) { this.agencyName = agencyName; }

    public String getMembershipRole() { return membershipRole; }
    public void setMembershipRole(String membershipRole) { this.membershipRole = membershipRole; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
