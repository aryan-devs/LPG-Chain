package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User query sent to the AI assistant")
public class AiChatRequestDTO {

    @NotBlank(message = "Message must not be blank")
    private String message;

    private String intentId;

    public AiChatRequestDTO() {}

    public AiChatRequestDTO(String message) {
        this.message = message;
    }

    public AiChatRequestDTO(String message, String intentId) {
        this.message = message;
        this.intentId = intentId;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getIntentId() { return intentId; }
    public void setIntentId(String intentId) { this.intentId = intentId; }
}

