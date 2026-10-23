package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to update complaint status and add resolution note")
public class ComplaintStatusUpdateDTO {

    @NotBlank(message = "Status is required (OPEN, IN_PROGRESS, RESOLVED, REJECTED)")
    private String status;

    private String resolutionNote;

    public ComplaintStatusUpdateDTO() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
}
