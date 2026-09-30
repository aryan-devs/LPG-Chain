package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.dto.VehicleLocationRequestDTO;
import com.lpgchain.dto.VehicleLocationResponseDTO;
import com.lpgchain.exception.ErrorResponse;
import com.lpgchain.exception.ResourceNotFoundException;
import com.lpgchain.service.VehicleTrackingService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tracking")
@Tag(name = "GPS Tracking", description = "Real-time vehicle GPS telemetry, tracking history, delivery tracking, and Traccar integration")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class VehicleTrackingController {

    private final VehicleTrackingService vehicleTrackingService;

    public VehicleTrackingController(VehicleTrackingService vehicleTrackingService) {
        this.vehicleTrackingService = vehicleTrackingService;
    }

    @PostMapping("/vehicles/{vehicleId}/location")
    @Operation(summary = "Record Vehicle Location", description = "Ingests a GPS telemetry position for a vehicle. Restricted to ADMIN and the vehicle's DISTRIBUTOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Location recorded successfully",
                    content = @Content(schema = @Schema(implementation = VehicleLocationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or invalid GPS coordinates",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden: User cannot ingest coordinates for this vehicle",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehicleLocationResponseDTO> recordVehicleLocation(
            @Parameter(description = "Vehicle identifier", example = "1") @PathVariable Long vehicleId,
            @Valid @RequestBody VehicleLocationRequestDTO dto) {
        VehicleLocationResponseDTO recorded = vehicleTrackingService.recordLocation(vehicleId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }

    @GetMapping("/vehicles/{vehicleId}/location")
    @Operation(summary = "Get Latest Vehicle Location", description = "Retrieves the latest GPS position of a vehicle. Accessible by ADMIN and the vehicle's DISTRIBUTOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Latest location retrieved",
                    content = @Content(schema = @Schema(implementation = VehicleLocationResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle or location not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehicleLocationResponseDTO> getLatestVehicleLocation(
            @Parameter(description = "Vehicle identifier", example = "1") @PathVariable Long vehicleId) {
        return vehicleTrackingService.getLatestLocation(vehicleId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No location records found for vehicle ID: " + vehicleId));
    }

    @GetMapping("/vehicles/{vehicleId}/history")
    @Operation(summary = "Get Vehicle Location History", description = "Retrieves location history for a vehicle ordered newest first with limit. Accessible by ADMIN and the vehicle's DISTRIBUTOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location history retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = VehicleLocationResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<VehicleLocationResponseDTO>> getVehicleLocationHistory(
            @Parameter(description = "Vehicle identifier", example = "1") @PathVariable Long vehicleId,
            @Parameter(description = "Maximum number of location records to return (1-100, default 50)", example = "50")
            @RequestParam(defaultValue = "50") int limit) {
        List<VehicleLocationResponseDTO> history = vehicleTrackingService.getLocationHistory(vehicleId, limit);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/deliveries/{deliveryId}/location")
    @Operation(summary = "Get Delivery Vehicle Location", description = "Retrieves the latest GPS position of the vehicle assigned to a delivery. Accessible by the customer of the booking, the distributor, or ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery vehicle location retrieved",
                    content = @Content(schema = @Schema(implementation = VehicleLocationResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden: User cannot access this delivery",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Delivery, assigned vehicle, or location not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehicleLocationResponseDTO> getDeliveryLocation(
            @Parameter(description = "Delivery identifier", example = "10") @PathVariable Long deliveryId) {
        return vehicleTrackingService.getLatestLocationByDeliveryId(deliveryId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No location records available for delivery ID: " + deliveryId));
    }

    @PatchMapping("/vehicles/{vehicleId}/traccar-device")
    @Operation(summary = "Update Traccar Device Mapping", description = "Associates or updates the Traccar device ID for a vehicle. Restricted to ADMIN and the vehicle's DISTRIBUTOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device ID mapped successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Object>> setTraccarDeviceId(
            @Parameter(description = "Vehicle identifier", example = "1") @PathVariable Long vehicleId,
            @RequestBody Map<String, String> body) {
        String traccarDeviceId = body != null ? body.get("traccarDeviceId") : null;
        vehicleTrackingService.setTraccarDeviceId(vehicleId, traccarDeviceId);
        return ResponseEntity.ok(Map.of(
                "vehicleId", vehicleId,
                "traccarDeviceId", traccarDeviceId != null ? traccarDeviceId : "",
                "message", "Traccar device ID updated successfully"
        ));
    }

    @PostMapping("/vehicles/{vehicleId}/sync-traccar")
    @Operation(summary = "Sync Location from Traccar", description = "Fetches the latest GPS position from the external Traccar server and saves it for this vehicle.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location synced from Traccar",
                    content = @Content(schema = @Schema(implementation = VehicleLocationResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vehicle or Traccar position not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehicleLocationResponseDTO> syncLocationFromTraccar(
            @Parameter(description = "Vehicle identifier", example = "1") @PathVariable Long vehicleId) {
        return vehicleTrackingService.syncLocationFromTraccar(vehicleId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No location available from Traccar for vehicle ID: " + vehicleId));
    }
}
