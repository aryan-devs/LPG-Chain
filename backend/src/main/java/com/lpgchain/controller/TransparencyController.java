package com.lpgchain.controller;

import com.lpgchain.dto.TransparencyReportDTO;
import com.lpgchain.entity.CylinderCategory;
import com.lpgchain.exception.ErrorResponse;
import com.lpgchain.service.CylinderAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * TransparencyController — Public and customer-facing REST APIs for Cylinder Allocation
 * & Sales Transparency figures.
 *
 * Exposes category-wise allocated, delivered, and remaining quantities.
 * Completely excludes internal credentials, passkeys, and customer personal information.
 */
@RestController
@RequestMapping("/api/transparency")
@Tag(name = "Allocation & Sales Transparency", description = "Public & customer transparency reports for cylinder quotas and verified completed sales")
public class TransparencyController {

    private final CylinderAllocationService allocationService;

    public TransparencyController(CylinderAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @GetMapping("/distributors/{distributorId}")
    @Operation(summary = "Get Distributor Transparency Report",
               description = "Returns official cylinder allocation, actual completed sales/deliveries, and remaining allocation balance for a distributor and reporting period.",
               security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transparency report successfully computed and retrieved",
                    content = @Content(schema = @Schema(implementation = TransparencyReportDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid reporting period or query parameter",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Distributor not found")
    })
    public ResponseEntity<TransparencyReportDTO> getDistributorTransparency(
            @Parameter(description = "Distributor ID", example = "1", required = true)
            @PathVariable Long distributorId,
            @Parameter(description = "Reporting calendar year (e.g. 2026)", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "Reporting calendar month (1-12)", example = "10")
            @RequestParam(required = false) Integer month,
            @Parameter(description = "Optional cylinder category filter (DOMESTIC_14_2, DOMESTIC_5_0, COMMERCIAL_19_0)", example = "DOMESTIC_14_2")
            @RequestParam(required = false) String category) {

        TransparencyReportDTO report = allocationService.getTransparencyReport(distributorId, year, month, category);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/periods")
    @Operation(summary = "Get Available Reporting Periods",
               description = "Retrieves all historical reporting periods with published cylinder allocations.",
               security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of reporting periods retrieved (YYYY-MM)",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
    })
    public ResponseEntity<List<String>> getReportingPeriods() {
        return ResponseEntity.ok(allocationService.getAvailableReportingPeriods());
    }

    @GetMapping("/categories")
    @Operation(summary = "Get Supported Cylinder Categories",
               description = "Lists standard cylinder categories, capacities, and target market classifications.",
               security = {})
    public ResponseEntity<List<Map<String, Object>>> getCategories() {
        List<Map<String, Object>> categories = Arrays.stream(CylinderCategory.values())
                .map(c -> Map.<String, Object>of(
                        "code", c.getCode(),
                        "displayName", c.getDisplayName(),
                        "capacityKg", c.getCapacityKg(),
                        "serviceType", c.getServiceType()
                ))
                .toList();
        return ResponseEntity.ok(categories);
    }
}
