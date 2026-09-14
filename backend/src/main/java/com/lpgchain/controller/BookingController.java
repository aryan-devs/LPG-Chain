package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.dto.BookingRequestDTO;
import com.lpgchain.dto.BookingResponseDTO;
import com.lpgchain.exception.ErrorResponse;
import com.lpgchain.service.BookingService;
import com.lpgchain.service.BookingService.StatusUpdateResult;
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
import java.util.Map;

/**
 * BookingController — REST controller for managing LPG cylinder bookings.
 * Enforces RBAC, ReBAC (ownership / distributor assignment), and ABAC (status transition lifecycle).
 */
@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "LPG cylinder booking lifecycle, tracking, and delivery management")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    @Operation(summary = "Get All Bookings", description = "Retrieves all bookings across the platform. Restricted to administrators.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of bookings retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookingResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN authority",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<BookingResponseDTO>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Booking by ID", description = "Retrieves a booking by ID. Caller must be the owning customer, assigned distributor member, or an administrator.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking found",
                    content = @Content(schema = @Schema(implementation = BookingResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - ReBAC relationship check failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponseDTO> getBookingById(
            @Parameter(description = "Booking unique identifier", example = "1001") @PathVariable Long id) {
        return bookingService.getBookingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get Bookings by Customer ID", description = "Retrieves bookings for a customer. Customer can only access their own bookings; Administrator can view any.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer bookings retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookingResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cannot access another customer's bookings",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByCustomer(
            @Parameter(description = "Customer user ID", example = "1") @PathVariable Long customerId) {
        return ResponseEntity.ok(bookingService.getBookingsByCustomerId(customerId));
    }

    @GetMapping("/distributor/{distributorId}")
    @Operation(summary = "Get Bookings by Distributor ID", description = "Retrieves bookings assigned to a distributor agency. Caller must be an active member of the distributor or an administrator.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distributor bookings retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BookingResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Cross-organization access prohibited",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByDistributor(
            @Parameter(description = "Distributor agency ID", example = "1") @PathVariable Long distributorId) {
        return ResponseEntity.ok(bookingService.getBookingsByDistributorId(distributorId));
    }

    @PostMapping
    @Operation(summary = "Create Cylinder Booking", description = "Creates a new LPG cylinder booking. Authenticated customers can only create bookings for themselves; Administrators can create for any customer.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created successfully",
                    content = @Content(schema = @Schema(implementation = BookingResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Privilege escalation attempt (e.g. creating booking for another customer)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<?> createBooking(@Valid @RequestBody BookingRequestDTO bookingRequest) {
        StatusUpdateResult result = bookingService.createBookingFromDTO(bookingRequest);

        return switch (result.getType()) {
            case SUCCESS   -> ResponseEntity.status(HttpStatus.CREATED).body(result.getBooking());
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Customer not found with id: " + bookingRequest.getCustomerId());
            case INVALID   -> ResponseEntity.badRequest().body(result.getErrorMessage());
        };
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update Booking Status", description = "Updates booking status. Enforces ABAC state transitions (PENDING -> CONFIRMED -> DELIVERED, or PENDING -> CANCELLED). Customers may only CANCEL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully",
                    content = @Content(schema = @Schema(implementation = BookingResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status transition (ABAC)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Caller not authorized to perform this status transition",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<?> updateBookingStatus(
            @Parameter(description = "Booking ID", example = "1001") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body("Field 'status' is required.");
        }

        StatusUpdateResult result = bookingService.updateBookingStatus(id, newStatus.toUpperCase());

        return switch (result.getType()) {
            case SUCCESS   -> ResponseEntity.ok(result.getBooking());
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case INVALID   -> ResponseEntity.badRequest().body(result.getErrorMessage());
        };
    }

    @PatchMapping("/{bookingId}/assign-distributor/{distributorId}")
    @Operation(summary = "Assign Distributor to Booking", description = "Assigns an agency to an existing PENDING cylinder booking. Restricted to administrators.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Distributor assigned successfully",
                    content = @Content(schema = @Schema(implementation = BookingResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Booking not in PENDING state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN authority",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Booking or Distributor not found")
    })
    public ResponseEntity<?> assignDistributor(
            @Parameter(description = "Booking ID", example = "1001") @PathVariable Long bookingId,
            @Parameter(description = "Distributor ID", example = "1") @PathVariable Long distributorId) {
        StatusUpdateResult result = bookingService.assignDistributor(bookingId, distributorId);

        return switch (result.getType()) {
            case SUCCESS   -> ResponseEntity.ok(result.getBooking());
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case INVALID   -> ResponseEntity.badRequest().body(result.getErrorMessage());
        };
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Booking", description = "Deletes a booking record. Restricted to administrators.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Booking deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN authority",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteBooking(
            @Parameter(description = "Booking ID", example = "1001") @PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
}
