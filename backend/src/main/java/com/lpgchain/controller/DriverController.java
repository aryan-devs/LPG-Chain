package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.dto.DriverRequestDTO;
import com.lpgchain.dto.DriverResponseDTO;
import com.lpgchain.exception.ErrorResponse;
import com.lpgchain.service.DriverService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@Tag(name = "Drivers", description = "Driver fleet management and assignment")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @Operation(summary = "Register Driver", description = "Registers a new delivery driver for a distributor. Requires ADMIN or member of the distributor.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Driver created",
                    content = @Content(schema = @Schema(implementation = DriverResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation or business rule failure",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DriverResponseDTO> createDriver(@Valid @RequestBody DriverRequestDTO dto) {
        DriverResponseDTO created = driverService.createDriver(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Driver by ID", description = "Retrieves a driver by ID. Requires ADMIN or member of the driver's distributor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver found",
                    content = @Content(schema = @Schema(implementation = DriverResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    public ResponseEntity<DriverResponseDTO> getDriverById(
            @Parameter(description = "Driver unique identifier", example = "10") @PathVariable Long id) {
        return driverService.getDriverById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get All Drivers", description = "Lists drivers. Admins see all; distributors see their own active fleet.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Drivers retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DriverResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<DriverResponseDTO>> getAllDrivers() {
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    @GetMapping("/distributor/{distributorId}")
    @Operation(summary = "Get Drivers by Distributor", description = "Retrieves all drivers for a specific distributor organization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Drivers retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DriverResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not a member of the distributor",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<DriverResponseDTO>> getDriversByDistributor(
            @Parameter(description = "Distributor ID", example = "1") @PathVariable Long distributorId) {
        return ResponseEntity.ok(driverService.getDriversByDistributorId(distributorId));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update Driver", description = "Updates driver details or status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver updated",
                    content = @Content(schema = @Schema(implementation = DriverResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid update payload or status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    public ResponseEntity<DriverResponseDTO> updateDriver(
            @Parameter(description = "Driver unique identifier", example = "10") @PathVariable Long id,
            @RequestBody DriverRequestDTO dto) {
        return driverService.updateDriver(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Driver", description = "Deletes a driver record.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Driver deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Driver not found")
    })
    public ResponseEntity<Void> deleteDriver(
            @Parameter(description = "Driver unique identifier", example = "10") @PathVariable Long id) {
        boolean deleted = driverService.deleteDriver(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
