package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload for updating a delivery's lifecycle status")
public class DeliveryStatusUpdateDTO {

    @NotBlank(message = "status is required")
    @Schema(description = "New lifecycle status (PICKED_UP, OUT_FOR_DELIVERY, DELIVERED)", example = "PICKED_UP", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    public DeliveryStatusUpdateDTO() {
    }

    public DeliveryStatusUpdateDTO(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
