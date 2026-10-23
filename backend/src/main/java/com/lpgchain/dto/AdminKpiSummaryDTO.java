package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AdminKpiSummaryDTO — High-level platform KPI counters for Admin Portal overview.
 * Exposes real aggregate statistics across all registered users, distributors, bookings, and inventory.
 */
@Schema(description = "High-level platform KPI counters for Admin Portal overview")
public class AdminKpiSummaryDTO {

    @Schema(description = "Total registered customer accounts", example = "150")
    private long totalCustomers;

    @Schema(description = "Total registered distributor agencies", example = "12")
    private long totalDistributors;

    @Schema(description = "Total platform bookings", example = "450")
    private long totalBookings;

    @Schema(description = "Total pending bookings awaiting fulfillment", example = "35")
    private long pendingBookings;

    @Schema(description = "Active deliveries in transit", example = "18")
    private long activeDeliveries;

    @Schema(description = "Successfully delivered bookings", example = "390")
    private long completedDeliveries;

    @Schema(description = "Total cylinders across all physical distributor inventories", example = "2400")
    private long totalCylinderInventory;

    @Schema(description = "Active system alerts or pending verification items", example = "2")
    private long activeAlerts;

    @Schema(description = "Indicates whether metrics are placeholder/mock (always false for live endpoints)", example = "false")
    private boolean isPlaceholder = false;

    public AdminKpiSummaryDTO() {
    }

    public AdminKpiSummaryDTO(long totalCustomers, long totalDistributors, long totalBookings,
                              long pendingBookings, long activeDeliveries, long completedDeliveries,
                              long totalCylinderInventory, long activeAlerts) {
        this.totalCustomers = totalCustomers;
        this.totalDistributors = totalDistributors;
        this.totalBookings = totalBookings;
        this.pendingBookings = pendingBookings;
        this.activeDeliveries = activeDeliveries;
        this.completedDeliveries = completedDeliveries;
        this.totalCylinderInventory = totalCylinderInventory;
        this.activeAlerts = activeAlerts;
        this.isPlaceholder = false;
    }

    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }

    public long getTotalDistributors() { return totalDistributors; }
    public void setTotalDistributors(long totalDistributors) { this.totalDistributors = totalDistributors; }

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public long getPendingBookings() { return pendingBookings; }
    public void setPendingBookings(long pendingBookings) { this.pendingBookings = pendingBookings; }

    public long getActiveDeliveries() { return activeDeliveries; }
    public void setActiveDeliveries(long activeDeliveries) { this.activeDeliveries = activeDeliveries; }

    public long getCompletedDeliveries() { return completedDeliveries; }
    public void setCompletedDeliveries(long completedDeliveries) { this.completedDeliveries = completedDeliveries; }

    public long getTotalCylinderInventory() { return totalCylinderInventory; }
    public void setTotalCylinderInventory(long totalCylinderInventory) { this.totalCylinderInventory = totalCylinderInventory; }

    public long getActiveAlerts() { return activeAlerts; }
    public void setActiveAlerts(long activeAlerts) { this.activeAlerts = activeAlerts; }

    public boolean getIsPlaceholder() { return isPlaceholder; }
    public void setIsPlaceholder(boolean isPlaceholder) { this.isPlaceholder = isPlaceholder; }
}
