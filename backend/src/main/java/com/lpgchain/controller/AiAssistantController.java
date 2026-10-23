package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.dto.AiChatRequestDTO;
import com.lpgchain.dto.AiChatResponseDTO;
import com.lpgchain.service.OllamaAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/assistant")
@Tag(name = "AI Operations Assistant", description = "Endpoints for conversational natural-language operations powered by local Ollama with database tools")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class AiAssistantController {

    private final OllamaAssistantService ollamaAssistantService;

    public AiAssistantController(OllamaAssistantService ollamaAssistantService) {
        this.ollamaAssistantService = ollamaAssistantService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Ask the AI assistant a question about bookings, payments, KYC, complaints, or demand forecasts")
    public ResponseEntity<AiChatResponseDTO> chat(@Valid @RequestBody AiChatRequestDTO request) {
        return ResponseEntity.ok(ollamaAssistantService.chat(request));
    }
}
