package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.dto.DistributorMembershipRequestDTO;
import com.lpgchain.dto.DistributorMembershipResponseDTO;
import com.lpgchain.exception.ErrorResponse;
import com.lpgchain.service.DistributorMembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * DistributorMembershipController — REST controller for managing distributor memberships.
 */
@RestController
@RequestMapping("/api/memberships")
@Tag(name = "Distributor Memberships", description = "Distributor organization membership management (ReBAC)")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class DistributorMembershipController {

    private final DistributorMembershipService distributorMembershipService;

    public DistributorMembershipController(DistributorMembershipService distributorMembershipService) {
        this.distributorMembershipService = distributorMembershipService;
    }

    @GetMapping("/my")
    @Operation(summary = "Get My Memberships", description = "Returns all distributor memberships for the authenticated user. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of current user memberships",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DistributorMembershipResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<DistributorMembershipResponseDTO>> getMyMemberships() {
        return ResponseEntity.ok(distributorMembershipService.getCurrentUserMemberships());
    }

    @GetMapping("/distributor/{distributorId}")
    @Operation(summary = "Get Memberships for Distributor", description = "Returns memberships associated with a specific distributor agency. Enforces ReBAC boundaries.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of memberships for the distributor",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DistributorMembershipResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cross-organization access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<DistributorMembershipResponseDTO>> getMembershipsForDistributor(
            @Parameter(description = "Distributor ID", example = "1") @PathVariable Long distributorId) {
        return ResponseEntity.ok(distributorMembershipService.getMembershipsForDistributor(distributorId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Membership by ID", description = "Returns membership details by record ID. Enforces ReBAC authorization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membership found",
                    content = @Content(schema = @Schema(implementation = DistributorMembershipResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - User is not authorized to access this membership",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Membership not found")
    })
    public ResponseEntity<DistributorMembershipResponseDTO> getMembershipById(
            @Parameter(description = "Membership ID", example = "10") @PathVariable Long id) {
        return distributorMembershipService.getMembershipById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create Distributor Membership", description = "Creates a new distributor membership. Caller must be an active OWNER of the distributor agency or ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Membership created successfully",
                    content = @Content(schema = @Schema(implementation = DistributorMembershipResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or duplicate membership",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller lacks OWNER privileges for target distributor",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DistributorMembershipResponseDTO> createMembership(
            @Valid @RequestBody DistributorMembershipRequestDTO request) {
        DistributorMembershipResponseDTO created = distributorMembershipService.createMembership(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Distributor Membership", description = "Removes a user's membership in a distributor agency. Requires OWNER or ADMIN authority.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Membership deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions to delete membership",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteMembership(
            @Parameter(description = "Membership ID", example = "10") @PathVariable Long id) {
        distributorMembershipService.deleteMembership(id);
        return ResponseEntity.noContent().build();
    }
}
