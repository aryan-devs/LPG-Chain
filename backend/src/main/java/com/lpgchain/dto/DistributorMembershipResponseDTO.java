package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * DistributorMembershipResponseDTO — Safe representation of a distributor membership
 * without exposing raw internal entity graph or sensitive user credentials.
 */
@Data
@Schema(description = "Representation of a user's membership within a distributor organization")
public class DistributorMembershipResponseDTO {

    @Schema(description = "Membership record ID", example = "10")
    private Long id;

    @Schema(description = "User ID", example = "5")
    private Long userId;

    @Schema(description = "User's full name", example = "Jane Doe")
    private String userName;

    @Schema(description = "User's email", example = "jane@distributor.example")
    private String userEmail;

    @Schema(description = "Distributor organization ID", example = "1")
    private Long distributorId;

    @Schema(description = "Distributor agency name", example = "City Gas Agency")
    private String distributorName;

    @Schema(description = "Membership role (OWNER, MANAGER, STAFF)", example = "STAFF")
    private String membershipRole;

    @Schema(description = "Membership status (ACTIVE, INACTIVE, PENDING)", example = "ACTIVE")
    private String status;

    public DistributorMembershipResponseDTO() {
    }

    public DistributorMembershipResponseDTO(Long id, Long userId, String userName, String userEmail,
                                            Long distributorId, String distributorName,
                                            String membershipRole, String status) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.distributorId = distributorId;
        this.distributorName = distributorName;
        this.membershipRole = membershipRole;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Long getDistributorId() { return distributorId; }
    public void setDistributorId(Long distributorId) { this.distributorId = distributorId; }

    public String getDistributorName() { return distributorName; }
    public void setDistributorName(String distributorName) { this.distributorName = distributorName; }

    public String getMembershipRole() { return membershipRole; }
    public void setMembershipRole(String membershipRole) { this.membershipRole = membershipRole; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
