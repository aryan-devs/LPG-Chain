package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.dto.DeliveryRequestDTO;
import com.lpgchain.dto.DeliveryResponseDTO;
import com.lpgchain.dto.DeliveryStatusUpdateDTO;
import com.lpgchain.exception.ErrorResponse;
import com.lpgchain.service.DeliveryService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@Tag(name = "Deliveries", description = "LPG delivery operations, driver/vehicle assignment, and lifecycle management")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping
    @Operation(summary = "Create Delivery", description = "Creates and schedules an LPG delivery assignment for a confirmed booking. Requires ADMIN or member of the assigned distributor.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Delivery created and assigned",
                    content = @Content(schema = @Schema(implementation = DeliveryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Business rule violation or validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller does not have permission",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DeliveryResponseDTO> createDelivery(@Valid @RequestBody DeliveryRequestDTO dto) {
        DeliveryResponseDTO created = deliveryService.createDelivery(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Delivery by ID", description = "Retrieves delivery details. Caller must be the owning customer, assigned distributor member, or administrator.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery details found",
                    content = @Content(schema = @Schema(implementation = DeliveryResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Relationship check failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<DeliveryResponseDTO> getDeliveryById(
            @Parameter(description = "Delivery unique identifier", example = "501") @PathVariable Long id) {
        return deliveryService.getDeliveryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get Customer Deliveries", description = "Retrieves all deliveries for a specific customer. Customer can only access their own.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer deliveries retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DeliveryResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot view another customer's deliveries",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<DeliveryResponseDTO>> getDeliveriesByCustomer(
            @Parameter(description = "Customer user ID", example = "101") @PathVariable Long customerId) {
        return ResponseEntity.ok(deliveryService.getDeliveriesByCustomerId(customerId));
    }

    @GetMapping("/distributor/{distributorId}")
    @Operation(summary = "Get Distributor Deliveries", description = "Retrieves all deliveries handled by a specific distributor organization.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distributor deliveries retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DeliveryResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not a member of the distributor",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<DeliveryResponseDTO>> getDeliveriesByDistributor(
            @Parameter(description = "Distributor ID", example = "1") @PathVariable Long distributorId) {
        return ResponseEntity.ok(deliveryService.getDeliveriesByDistributorId(distributorId));
    }

    @GetMapping("/driver/{driverId}")
    @Operation(summary = "Get Driver Deliveries", description = "Retrieves all deliveries assigned to a specific driver.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver deliveries retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DeliveryResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized for this driver's organization",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<DeliveryResponseDTO>> getDeliveriesByDriver(
            @Parameter(description = "Driver ID", example = "10") @PathVariable Long driverId) {
        return ResponseEntity.ok(deliveryService.getDeliveriesByDriverId(driverId));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update Delivery Status", description = "Advances delivery status (ASSIGNED -> PICKED_UP -> OUT_FOR_DELIVERY -> DELIVERED). Restricted to distributor or admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery status updated",
                    content = @Content(schema = @Schema(implementation = DeliveryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status transition or terminal state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Customer cannot update delivery status",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<DeliveryResponseDTO> updateDeliveryStatus(
            @Parameter(description = "Delivery unique identifier", example = "501") @PathVariable Long id,
            @Valid @RequestBody DeliveryStatusUpdateDTO dto) {
        DeliveryResponseDTO updated = deliveryService.updateDeliveryStatus(id, dto.getStatus());
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/assign-driver/{driverId}")
    @Operation(summary = "Assign or Reassign Driver", description = "Assigns or changes the driver for an active delivery. Restricted to distributor or admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Driver assigned successfully",
                    content = @Content(schema = @Schema(implementation = DeliveryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Driver unavailable, mismatched distributor, or delivery completed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Delivery or driver not found")
    })
    public ResponseEntity<DeliveryResponseDTO> assignDriver(
            @Parameter(description = "Delivery unique identifier", example = "501") @PathVariable Long id,
            @Parameter(description = "New driver identifier", example = "10") @PathVariable Long driverId) {
        DeliveryResponseDTO updated = deliveryService.assignDriver(id, driverId);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/assign-vehicle/{vehicleId}")
    @Operation(summary = "Assign or Reassign Vehicle", description = "Assigns or changes the vehicle for an active delivery. Restricted to distributor or admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vehicle assigned successfully",
                    content = @Content(schema = @Schema(implementation = DeliveryResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Vehicle unavailable, mismatched distributor, or delivery completed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Delivery or vehicle not found")
    })
    public ResponseEntity<DeliveryResponseDTO> assignVehicle(
            @Parameter(description = "Delivery unique identifier", example = "501") @PathVariable Long id,
            @Parameter(description = "New vehicle identifier", example = "5") @PathVariable Long vehicleId) {
        DeliveryResponseDTO updated = deliveryService.assignVehicle(id, vehicleId);
        return ResponseEntity.ok(updated);
    }
}
