package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.dto.DistributorSelectionRequestDTO;
import com.lpgchain.dto.DistributorSelectionResponseDTO;
import com.lpgchain.dto.LoginRequestDTO;
import com.lpgchain.dto.LoginResponseDTO;
import com.lpgchain.dto.RoleSelectionRequestDTO;
import com.lpgchain.exception.ErrorResponse;
import com.lpgchain.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthController — Handles authentication and role/distributor selection endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication, active-role selection, and distributor context management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates user credentials and returns LoginResponseDTO on success.
     */
    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates user credentials using BCrypt + application pepper. Returns JWT token and active role on success.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or email policy violation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid email or password",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Selects an active role for an authenticated user and returns an active-role JWT.
     */
    @PostMapping("/select-role")
    @Operation(summary = "Select Active Role",
            description = "Selects an active role from user's assigned roles. Requires JWT authentication.",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active role selected successfully",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid role request or email policy violation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requested role is not assigned to user",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponseDTO> selectRole(@Valid @RequestBody RoleSelectionRequestDTO request) {
        LoginResponseDTO response = authService.selectRole(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Selects an active distributor organization context for an authenticated user and returns a scoped JWT.
     */
    @PostMapping("/select-distributor")
    @Operation(summary = "Select Distributor Organization Context",
            description = "Selects an active distributor organization for users with multiple distributor memberships. Requires JWT authentication.",
            security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distributor context selected successfully",
                    content = @Content(schema = @Schema(implementation = DistributorSelectionResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User lacks active membership in requested distributor",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DistributorSelectionResponseDTO> selectDistributor(
            @Valid @RequestBody DistributorSelectionRequestDTO request) {
        DistributorSelectionResponseDTO response = authService.selectDistributor(request);
        return ResponseEntity.ok(response);
    }
}
