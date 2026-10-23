package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.dto.AdminKpiSummaryDTO;
import com.lpgchain.entity.CylinderInventory;
import com.lpgchain.exception.ErrorResponse;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.CylinderInventoryRepository;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * AdminDashboardController — High-level platform KPIs and operational metrics for the Admin Portal.
 * Restricted strictly to users authenticated with the ROLE_ADMIN authority.
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Portal", description = "Platform-wide administrative metrics and management APIs")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final DistributorRepository distributorRepository;
    private final BookingRepository bookingRepository;
    private final DeliveryRepository deliveryRepository;
    private final CylinderInventoryRepository cylinderInventoryRepository;

    public AdminDashboardController(UserRepository userRepository,
                                    DistributorRepository distributorRepository,
                                    BookingRepository bookingRepository,
                                    DeliveryRepository deliveryRepository,
                                    CylinderInventoryRepository cylinderInventoryRepository) {
        this.userRepository = userRepository;
        this.distributorRepository = distributorRepository;
        this.bookingRepository = bookingRepository;
        this.deliveryRepository = deliveryRepository;
        this.cylinderInventoryRepository = cylinderInventoryRepository;
    }

    @GetMapping("/overview-kpis")
    @Operation(summary = "Get Platform Overview KPIs",
               description = "Returns platform-wide aggregate counts for customers, distributors, bookings, deliveries, and inventory. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Platform KPIs retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AdminKpiSummaryDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AdminKpiSummaryDTO> getOverviewKpis() {
        long totalUsers = userRepository.count();
        long totalDistributors = distributorRepository.count();
        long totalBookings = bookingRepository.count();

        // Calculate pending bookings
        long pendingBookings = bookingRepository.findAll().stream()
                .filter(b -> "PENDING".equalsIgnoreCase(b.getStatus()))
                .count();

        // Calculate active deliveries (IN_TRANSIT, ASSIGNED, OUT_FOR_DELIVERY)
        Set<String> activeDeliveryStatuses = Set.of("ASSIGNED", "OUT_FOR_DELIVERY", "IN_TRANSIT");
        long activeDeliveries = deliveryRepository.findAll().stream()
                .filter(d -> d.getStatus() != null && activeDeliveryStatuses.contains(d.getStatus().toUpperCase()))
                .count();

        // Calculate completed deliveries
        long completedDeliveries = deliveryRepository.findAll().stream()
                .filter(d -> "DELIVERED".equalsIgnoreCase(d.getStatus()))
                .count();

        // Total physical inventory (filled + empty stock across all depots)
        long totalInventory = cylinderInventoryRepository.findAll().stream()
                .mapToLong(inv -> (inv.getFilledStock() != null ? inv.getFilledStock() : 0) +
                                  (inv.getEmptyStock() != null ? inv.getEmptyStock() : 0))
                .sum();

        // Count pending distributor verifications or alerts
        long pendingDistributorVerifications = distributorRepository.findAll().stream()
                .filter(d -> "PENDING_VERIFICATION".equalsIgnoreCase(d.getStatus()))
                .count();

        AdminKpiSummaryDTO kpis = new AdminKpiSummaryDTO(
                totalUsers,
                totalDistributors,
                totalBookings,
                pendingBookings,
                activeDeliveries,
                completedDeliveries,
                totalInventory,
                pendingDistributorVerifications
        );

        return ResponseEntity.ok(kpis);
    }
}
