package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.dto.DistributorStatusUpdateDTO;
import com.lpgchain.dto.DistributorSummaryDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.exception.ErrorResponse;
import com.lpgchain.service.DistributorService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * DistributorController — REST controller for LPG distributor agency directory and management.
 */
@RestController
@RequestMapping("/api/distributors")
@Tag(name = "Distributors", description = "LPG distributor agency registration and directory")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class DistributorController {

    private final DistributorService distributorService;

    public DistributorController(DistributorService distributorService) {
        this.distributorService = distributorService;
    }

    @GetMapping("/public/search")
    @Operation(summary = "Search Distributors Directory", 
               description = "Public directory search for LPG distributor agencies by PIN code, city, and service type. Accessible anonymously.",
               security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matching distributor agencies retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DistributorSummaryDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters (e.g. unrecognized service type)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<DistributorSummaryDTO>> searchDistributors(
            @Parameter(description = "Postal PIN code filter", example = "400001")
            @RequestParam(required = false) String pinCode,
            @Parameter(description = "City name filter (case-insensitive)", example = "Mumbai")
            @RequestParam(required = false) String city,
            @Parameter(description = "Supported service type (DOMESTIC, COMMERCIAL, BOTH)", example = "BOTH")
            @RequestParam(required = false) String serviceType) {
        List<DistributorSummaryDTO> results = distributorService.searchDistributors(pinCode, city, serviceType);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/public/{id}")
    @Operation(summary = "Get Public Distributor Profile by ID",
               description = "Retrieves sanitized public distributor agency profile by ID. Accessible anonymously.",
               security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Public distributor agency profile retrieved",
                    content = @Content(schema = @Schema(implementation = DistributorSummaryDTO.class))),
            @ApiResponse(responseCode = "404", description = "Distributor agency not found")
    })
    public ResponseEntity<DistributorSummaryDTO> getPublicDistributorById(
            @Parameter(description = "Distributor ID", example = "1") @PathVariable Long id) {
        return distributorService.getDistributorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get All Distributors", description = "Retrieves all registered LPG distributor agencies. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of distributors retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DistributorSummaryDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<DistributorSummaryDTO>> getAllDistributors() {
        return ResponseEntity.ok(distributorService.getAllDistributors());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Distributor by ID", description = "Retrieves details of a distributor agency by ID. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distributor agency found",
                    content = @Content(schema = @Schema(implementation = DistributorSummaryDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Distributor not found")
    })
    public ResponseEntity<DistributorSummaryDTO> getDistributorById(
            @Parameter(description = "Distributor ID", example = "1") @PathVariable Long id) {
        return distributorService.getDistributorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create Distributor Agency", description = "Registers a new distributor agency. Requires ADMIN authority.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Distributor created",
                    content = @Content(schema = @Schema(implementation = DistributorSummaryDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DistributorSummaryDTO> createDistributor(@RequestBody Distributor distributor) {
        DistributorSummaryDTO savedDistributor = distributorService.saveDistributor(distributor);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDistributor);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Distributor Agency", description = "Deletes a distributor agency by ID. Requires ADMIN authority.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Distributor deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteDistributor(
            @Parameter(description = "Distributor ID", example = "1") @PathVariable Long id) {
        distributorService.deleteDistributor(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update Distributor Status",
               description = "Activates, suspends, or updates verification status of a distributor agency. Requires ADMIN authority.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distributor status updated successfully",
                    content = @Content(schema = @Schema(implementation = DistributorSummaryDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid status value",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Distributor not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DistributorSummaryDTO> updateDistributorStatus(
            @Parameter(description = "Distributor ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody DistributorStatusUpdateDTO request) {
        DistributorSummaryDTO updated = distributorService.updateDistributorStatus(id, request.getStatus());
        return ResponseEntity.ok(updated);
    }
}
