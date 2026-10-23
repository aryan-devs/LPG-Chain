package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.dto.AllocationAdjustmentRequestDTO;
import com.lpgchain.dto.AllocationAdjustmentResponseDTO;
import com.lpgchain.dto.CylinderAllocationRequestDTO;
import com.lpgchain.dto.CylinderAllocationResponseDTO;
import com.lpgchain.exception.ErrorResponse;
import com.lpgchain.service.CylinderAllocationService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CylinderAllocationController — REST endpoints for managing official cylinder allocation records
 * and auditable quota adjustments.
 *
 * Creation and mutations are restricted to ADMIN.
 * Viewing is permitted to ADMIN and the assigned DISTRIBUTOR.
 */
@RestController
@RequestMapping("/api/allocations")
@Tag(name = "Cylinder Allocations", description = "Official cylinder quota management and auditable adjustments")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class CylinderAllocationController {

    private final CylinderAllocationService allocationService;

    public CylinderAllocationController(CylinderAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @PostMapping
    @Operation(summary = "Create Cylinder Allocation",
               description = "Registers an official cylinder quota for a distributor, category, and month/year. Requires ADMIN authority.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cylinder allocation created successfully",
                    content = @Content(schema = @Schema(implementation = CylinderAllocationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or duplicate allocation for same period/category",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied — Requires ADMIN role")
    })
    public ResponseEntity<CylinderAllocationResponseDTO> createAllocation(
            @Valid @RequestBody CylinderAllocationRequestDTO dto) {
        CylinderAllocationResponseDTO created = allocationService.createAllocation(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Cylinder Allocation",
               description = "Updates allocated quantity or source reference of an existing allocation. Requires ADMIN authority.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Allocation updated successfully",
                    content = @Content(schema = @Schema(implementation = CylinderAllocationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid quantity or input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied — Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Allocation not found")
    })
    public ResponseEntity<CylinderAllocationResponseDTO> updateAllocation(
            @Parameter(description = "Allocation ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "New allocated quantity")
            @RequestParam(required = false) Integer quantity,
            @Parameter(description = "New or updated source reference")
            @RequestParam(required = false) String sourceReference) {
        CylinderAllocationResponseDTO updated = allocationService.updateAllocation(id, quantity, sourceReference);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/adjustments")
    @Operation(summary = "Record Auditable Quota Adjustment",
               description = "Appends an auditable quota correction, return, or emergency transfer to an existing allocation. Requires ADMIN authority.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Adjustment recorded successfully",
                    content = @Content(schema = @Schema(implementation = AllocationAdjustmentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid adjustment parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied — Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Allocation not found")
    })
    public ResponseEntity<AllocationAdjustmentResponseDTO> addAdjustment(
            @Parameter(description = "Allocation ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody AllocationAdjustmentRequestDTO dto) {
        AllocationAdjustmentResponseDTO created = allocationService.addAdjustment(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}/adjustments")
    @Operation(summary = "Get Allocation Adjustments",
               description = "Retrieves all auditable adjustments for an allocation record. Permitted to ADMIN or the assigned DISTRIBUTOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Adjustments retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AllocationAdjustmentResponseDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied — Not authorized for this distributor"),
            @ApiResponse(responseCode = "404", description = "Allocation not found")
    })
    public ResponseEntity<List<AllocationAdjustmentResponseDTO>> getAdjustments(
            @Parameter(description = "Allocation ID", required = true)
            @PathVariable Long id) {
        List<AllocationAdjustmentResponseDTO> adjustments = allocationService.getAdjustmentsForAllocation(id);
        return ResponseEntity.ok(adjustments);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Allocation by ID",
               description = "Retrieves an allocation record by its ID. Permitted to ADMIN or the assigned DISTRIBUTOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Allocation found",
                    content = @Content(schema = @Schema(implementation = CylinderAllocationResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Allocation not found")
    })
    public ResponseEntity<CylinderAllocationResponseDTO> getAllocationById(
            @Parameter(description = "Allocation ID", required = true)
            @PathVariable Long id) {
        CylinderAllocationResponseDTO alloc = allocationService.getAllocationById(id);
        return ResponseEntity.ok(alloc);
    }

    @GetMapping("/distributor/{distributorId}")
    @Operation(summary = "Get Distributor Allocations",
               description = "Retrieves all allocation records for a distributor, optionally filtered by year and month. Permitted to ADMIN or the assigned DISTRIBUTOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distributor allocations retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CylinderAllocationResponseDTO.class)))),
            @ApiResponse(responseCode = "403", description = "Access denied — Not authorized for this distributor")
    })
    public ResponseEntity<List<CylinderAllocationResponseDTO>> getDistributorAllocations(
            @Parameter(description = "Distributor ID", required = true)
            @PathVariable Long distributorId,
            @Parameter(description = "Reporting calendar year (e.g. 2026)")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Reporting calendar month (1-12)")
            @RequestParam(required = false) Integer month) {
        List<CylinderAllocationResponseDTO> allocations = allocationService.getDistributorAllocations(distributorId, year, month);
        return ResponseEntity.ok(allocations);
    }
}
