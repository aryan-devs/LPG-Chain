package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
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
 * DistributorController — REST controller for managing LPG distributor agencies.
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

    @GetMapping
    @Operation(summary = "Get All Distributors", description = "Retrieves all registered LPG distributor agencies. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of distributors retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Distributor.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<Distributor>> getAllDistributors() {
        return ResponseEntity.ok(distributorService.getAllDistributors());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Distributor by ID", description = "Retrieves details of a distributor agency by ID. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distributor agency found",
                    content = @Content(schema = @Schema(implementation = Distributor.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Distributor not found")
    })
    public ResponseEntity<Distributor> getDistributorById(
            @Parameter(description = "Distributor ID", example = "1") @PathVariable Long id) {
        return distributorService.getDistributorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create Distributor Agency", description = "Registers a new distributor agency. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Distributor created",
                    content = @Content(schema = @Schema(implementation = Distributor.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Distributor> createDistributor(@RequestBody Distributor distributor) {
        Distributor savedDistributor = distributorService.saveDistributor(distributor);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDistributor);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Distributor Agency", description = "Deletes a distributor agency by ID. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Distributor deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteDistributor(
            @Parameter(description = "Distributor ID", example = "1") @PathVariable Long id) {
        distributorService.deleteDistributor(id);
        return ResponseEntity.noContent().build();
    }
}
