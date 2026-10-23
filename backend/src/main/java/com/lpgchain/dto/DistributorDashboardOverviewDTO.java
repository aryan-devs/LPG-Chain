package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Aggregated operational overview metrics for distributor dashboard")
public class DistributorDashboardOverviewDTO {

    private Long distributorId;
    private String distributorName;
    private String distributorCode;

    // Booking & Order lifecycle counts
    private long newBookingRequestsCount; // PENDING
    private long acceptedQueuedOrdersCount; // CONFIRMED
    private long scheduledDeliveriesCount; // ASSIGNED
    private long dispatchedDeliveriesCount; // OUT_FOR_DELIVERY
    private long completedDeliveriesCount; // DELIVERED
    private long cancelledBookingsCount;

    // Inventory aggregate totals
    private int totalFilledStock;
    private int totalEmptyStock;
    private int totalReservedStock;
    private int totalDamagedStock;
    private int totalAvailableStock;

    // Allocation & Transparency aggregate totals
    private int allocatedMonthlyTotal;
    private int soldMonthlyTotal;
    private int remainingAllocationTotal;

    // Queue age & operations
    private double averageQueueAgeHours;
    private long overdueOrdersCount;

    // Category breakdown
    private List<CylinderInventoryResponseDTO> inventory = new ArrayList<>();
    private List<CategoryTransparencyDTO> allocations = new ArrayList<>();
    private List<BookingResponseDTO> recentBookings = new ArrayList<>();

    public DistributorDashboardOverviewDTO() {}

    public Long getDistributorId() { return distributorId; }
    public void setDistributorId(Long distributorId) { this.distributorId = distributorId; }

    public String getDistributorName() { return distributorName; }
    public void setDistributorName(String distributorName) { this.distributorName = distributorName; }

    public String getDistributorCode() { return distributorCode; }
    public void setDistributorCode(String distributorCode) { this.distributorCode = distributorCode; }

    public long getNewBookingRequestsCount() { return newBookingRequestsCount; }
    public void setNewBookingRequestsCount(long newBookingRequestsCount) { this.newBookingRequestsCount = newBookingRequestsCount; }

    public long getAcceptedQueuedOrdersCount() { return acceptedQueuedOrdersCount; }
    public void setAcceptedQueuedOrdersCount(long acceptedQueuedOrdersCount) { this.acceptedQueuedOrdersCount = acceptedQueuedOrdersCount; }

    public long getScheduledDeliveriesCount() { return scheduledDeliveriesCount; }
    public void setScheduledDeliveriesCount(long scheduledDeliveriesCount) { this.scheduledDeliveriesCount = scheduledDeliveriesCount; }

    public long getDispatchedDeliveriesCount() { return dispatchedDeliveriesCount; }
    public void setDispatchedDeliveriesCount(long dispatchedDeliveriesCount) { this.dispatchedDeliveriesCount = dispatchedDeliveriesCount; }

    public long getCompletedDeliveriesCount() { return completedDeliveriesCount; }
    public void setCompletedDeliveriesCount(long completedDeliveriesCount) { this.completedDeliveriesCount = completedDeliveriesCount; }

    public long getCancelledBookingsCount() { return cancelledBookingsCount; }
    public void setCancelledBookingsCount(long cancelledBookingsCount) { this.cancelledBookingsCount = cancelledBookingsCount; }

    public int getTotalFilledStock() { return totalFilledStock; }
    public void setTotalFilledStock(int totalFilledStock) { this.totalFilledStock = totalFilledStock; }

    public int getTotalEmptyStock() { return totalEmptyStock; }
    public void setTotalEmptyStock(int totalEmptyStock) { this.totalEmptyStock = totalEmptyStock; }

    public int getTotalReservedStock() { return totalReservedStock; }
    public void setTotalReservedStock(int totalReservedStock) { this.totalReservedStock = totalReservedStock; }

    public int getTotalDamagedStock() { return totalDamagedStock; }
    public void setTotalDamagedStock(int totalDamagedStock) { this.totalDamagedStock = totalDamagedStock; }

    public int getTotalAvailableStock() { return totalAvailableStock; }
    public void setTotalAvailableStock(int totalAvailableStock) { this.totalAvailableStock = totalAvailableStock; }

    public int getAllocatedMonthlyTotal() { return allocatedMonthlyTotal; }
    public void setAllocatedMonthlyTotal(int allocatedMonthlyTotal) { this.allocatedMonthlyTotal = allocatedMonthlyTotal; }

    public int getSoldMonthlyTotal() { return soldMonthlyTotal; }
    public void setSoldMonthlyTotal(int soldMonthlyTotal) { this.soldMonthlyTotal = soldMonthlyTotal; }

    public int getRemainingAllocationTotal() { return remainingAllocationTotal; }
    public void setRemainingAllocationTotal(int remainingAllocationTotal) { this.remainingAllocationTotal = remainingAllocationTotal; }

    public double getAverageQueueAgeHours() { return averageQueueAgeHours; }
    public void setAverageQueueAgeHours(double averageQueueAgeHours) { this.averageQueueAgeHours = averageQueueAgeHours; }

    public long getOverdueOrdersCount() { return overdueOrdersCount; }
    public void setOverdueOrdersCount(long overdueOrdersCount) { this.overdueOrdersCount = overdueOrdersCount; }

    public List<CylinderInventoryResponseDTO> getInventory() { return inventory; }
    public void setInventory(List<CylinderInventoryResponseDTO> inventory) { this.inventory = inventory; }

    public List<CategoryTransparencyDTO> getAllocations() { return allocations; }
    public void setAllocations(List<CategoryTransparencyDTO> allocations) { this.allocations = allocations; }

    public List<BookingResponseDTO> getRecentBookings() { return recentBookings; }
    public void setRecentBookings(List<BookingResponseDTO> recentBookings) { this.recentBookings = recentBookings; }
}
