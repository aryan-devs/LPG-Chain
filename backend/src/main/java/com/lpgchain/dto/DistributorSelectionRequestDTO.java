package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DistributorSelectionRequestDTO — Request body for selecting an active distributor organization context.
 */
@Data
@Schema(description = "Request payload for selecting active distributor organization context")
public class DistributorSelectionRequestDTO {

    @NotNull(message = "Distributor ID is required")
    @Schema(description = "Unique ID of the distributor agency", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long distributorId;

    public DistributorSelectionRequestDTO() {
    }

    public DistributorSelectionRequestDTO(Long distributorId) {
        this.distributorId = distributorId;
    }

    public Long getDistributorId() {
        return distributorId;
    }

    public void setDistributorId(Long distributorId) {
        this.distributorId = distributorId;
    }
}
