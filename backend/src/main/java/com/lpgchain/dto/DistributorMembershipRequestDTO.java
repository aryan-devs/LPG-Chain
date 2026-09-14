package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DistributorMembershipRequestDTO — Payload for creating or updating a distributor membership.
 */
@Data
@Schema(description = "Request payload for creating a distributor organization membership")
public class DistributorMembershipRequestDTO {

    @NotNull(message = "User ID is required")
    @Schema(description = "User ID to grant membership to", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @NotNull(message = "Distributor ID is required")
    @Schema(description = "Target distributor organization ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long distributorId;

    @NotBlank(message = "Membership role is required")
    @Schema(description = "Organizational role (OWNER, MANAGER, STAFF)", example = "STAFF", requiredMode = Schema.RequiredMode.REQUIRED)
    private String membershipRole; // OWNER, MANAGER, STAFF

    @Schema(description = "Membership status (ACTIVE, INACTIVE, PENDING)", example = "ACTIVE")
    private String status; // ACTIVE, INACTIVE, PENDING (defaults to ACTIVE)

    public DistributorMembershipRequestDTO() {
    }

    public DistributorMembershipRequestDTO(Long userId, Long distributorId, String membershipRole, String status) {
        this.userId = userId;
        this.distributorId = distributorId;
        this.membershipRole = membershipRole;
        this.status = status;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getDistributorId() { return distributorId; }
    public void setDistributorId(Long distributorId) { this.distributorId = distributorId; }

    public String getMembershipRole() { return membershipRole; }
    public void setMembershipRole(String membershipRole) { this.membershipRole = membershipRole; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
