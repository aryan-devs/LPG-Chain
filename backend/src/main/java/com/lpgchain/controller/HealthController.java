package com.lpgchain.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HealthController — Provides public health check status.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "System health check and connectivity monitoring")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "System Health Check", description = "Returns system operational status. Publicly accessible without authentication.")
    @ApiResponse(responseCode = "200", description = "System is operational")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "LPG-Chain Backend");
        return ResponseEntity.ok(response);
    }
}
