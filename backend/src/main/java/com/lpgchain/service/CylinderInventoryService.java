package com.lpgchain.service;

import com.lpgchain.dto.BookingResponseDTO;
import com.lpgchain.dto.CategoryTransparencyDTO;
import com.lpgchain.dto.CylinderInventoryResponseDTO;
import com.lpgchain.dto.CylinderInventoryUpdateDTO;
import com.lpgchain.dto.DistributorDashboardOverviewDTO;
import com.lpgchain.dto.InventoryMovementResponseDTO;
import com.lpgchain.dto.TransparencyReportDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.CylinderCategory;
import com.lpgchain.entity.CylinderInventory;
import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.InventoryMovement;
import com.lpgchain.entity.User;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.CylinderInventoryRepository;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.InventoryMovementRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CylinderInventoryService — Manages physical depot inventory, stock movement auditing,
 * and distributor operational dashboard summaries.
 *
 * Enforces strict ReBAC: Only active distributor members or administrators can inspect/adjust depot inventory.
 */
@Service
@Transactional(readOnly = true)
public class CylinderInventoryService {

    private final CylinderInventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final DistributorRepository distributorRepository;
    private final BookingRepository bookingRepository;
    private final DeliveryRepository deliveryRepository;
    private final CylinderAllocationService allocationService;
    private final BookingAuthorizationService authorizationService;

    public CylinderInventoryService(CylinderInventoryRepository inventoryRepository,
                                   InventoryMovementRepository movementRepository,
                                   DistributorRepository distributorRepository,
                                   BookingRepository bookingRepository,
                                   DeliveryRepository deliveryRepository,
                                   CylinderAllocationService allocationService,
                                   BookingAuthorizationService authorizationService) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.distributorRepository = distributorRepository;
        this.bookingRepository = bookingRepository;
        this.deliveryRepository = deliveryRepository;
        this.allocationService = allocationService;
        this.authorizationService = authorizationService;
    }

    /**
     * ReBAC check: verifies caller is authorized for the given distributor.
     */
    public void checkDistributorAccess(Long distributorId) {
        if (authorizationService.isAdmin(authorizationService.getAuthentication())) {
            return;
        }
        User user = authorizationService.getAuthenticatedUser();
        if (!authorizationService.isDistributorMember(distributorId, user)) {
            throw new AccessDeniedException("Forbidden: User is not authorized for distributor agency #" + distributorId);
        }
    }

    /**
     * Retrieves category-wise physical depot inventory for a distributor.
     * Initializes default records if none exist yet.
     */
    @Transactional
    public List<CylinderInventoryResponseDTO> getDistributorInventory(Long distributorId) {
        checkDistributorAccess(distributorId);

        Distributor distributor = distributorRepository.findById(distributorId)
                .orElseThrow(() -> new IllegalArgumentException("Distributor not found with ID: " + distributorId));

        List<CylinderInventory> list = inventoryRepository.findByDistributorId(distributorId);

        // Ensure canonical categories exist
        CylinderCategory[] categories = CylinderCategory.values();
        for (CylinderCategory cat : categories) {
            boolean exists = list.stream().anyMatch(inv -> cat.getCode().equalsIgnoreCase(inv.getCylinderCategory()));
            if (!exists) {
                CylinderInventory init = new CylinderInventory(
                        distributor, cat.getCode(), cat.getCapacityKg(), 0, 0, 0, 0, "system");
                CylinderInventory saved = inventoryRepository.save(init);
                list.add(saved);
            }
        }

        // Calculate active reserved stock dynamically from queued CONFIRMED bookings
        List<Booking> confirmedBookings = bookingRepository.findByDistributorIdAndStatus(distributorId, "CONFIRMED");
        Map<String, Integer> reservedMap = confirmedBookings.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getCylinderCategory() != null ? CylinderCategory.fromCode(b.getCylinderCategory()).getCode() : CylinderCategory.DOMESTIC_14_2.getCode(),
                        Collectors.summingInt(b -> b.getQuantity() != null && b.getQuantity() > 0 ? b.getQuantity() : 1)
                ));

        return list.stream()
                .map(inv -> {
                    int reserved = reservedMap.getOrDefault(inv.getCylinderCategory(), 0);
                    inv.setReservedStock(reserved);
                    return mapToDTO(inv);
                })
                .sorted(Comparator.comparing(CylinderInventoryResponseDTO::getCylinderCategory))
                .collect(Collectors.toList());
    }

    /**
     * Updates physical stock counts and records an auditable InventoryMovement.
     */
    @Transactional
    public CylinderInventoryResponseDTO updateStock(Long distributorId, CylinderInventoryUpdateDTO dto) {
        checkDistributorAccess(distributorId);

        Distributor distributor = distributorRepository.findById(distributorId)
                .orElseThrow(() -> new IllegalArgumentException("Distributor not found with ID: " + distributorId));

        CylinderCategory category = CylinderCategory.fromCode(dto.getCylinderCategory());

        CylinderInventory inventory = inventoryRepository.findByDistributorIdAndCylinderCategory(distributorId, category.getCode())
                .orElseGet(() -> new CylinderInventory(distributor, category.getCode(), category.getCapacityKg(), 0, 0, 0, 0, "system"));

        int oldFilled = inventory.getFilledStock() != null ? inventory.getFilledStock() : 0;
        int newFilled = dto.getFilledStock();
        int diff = newFilled - oldFilled;

        inventory.setFilledStock(newFilled);
        inventory.setEmptyStock(dto.getEmptyStock());
        inventory.setDamagedStock(dto.getDamagedStock() != null ? dto.getDamagedStock() : 0);

        User currentUser = authorizationService.getAuthenticatedUser();
        String actor = currentUser != null ? currentUser.getEmail() : "distributor";
        inventory.setLastAuditedBy(actor);
        inventory.setLastAuditedAt(LocalDateTime.now());

        // Recalculate reserved
        List<Booking> confirmedBookings = bookingRepository.findByDistributorIdAndStatus(distributorId, "CONFIRMED");
        int reserved = confirmedBookings.stream()
                .filter(b -> category.getCode().equalsIgnoreCase(b.getCylinderCategory()))
                .mapToInt(b -> b.getQuantity() != null && b.getQuantity() > 0 ? b.getQuantity() : 1)
                .sum();

        inventory.setReservedStock(reserved);

        CylinderInventory saved = inventoryRepository.save(inventory);

        // Record auditable inventory movement
        String movementType = diff >= 0 ? "STOCK_RECEIPT" : "AUDIT_ADJUSTMENT";
        String reason = dto.getReason() != null && !dto.getReason().isBlank()
                ? dto.getReason()
                : "Physical depot inventory adjustment";

        InventoryMovement movement = new InventoryMovement(
                saved,
                movementType,
                diff,
                reason,
                dto.getReferenceNumber(),
                actor
        );
        movementRepository.save(movement);

        return mapToDTO(saved);
    }

    /**
     * Fetches movement history for an inventory category.
     */
    public List<InventoryMovementResponseDTO> getInventoryMovements(Long distributorId, String categoryCode) {
        checkDistributorAccess(distributorId);

        CylinderCategory category = CylinderCategory.fromCode(categoryCode);
        Optional<CylinderInventory> invOpt = inventoryRepository.findByDistributorIdAndCylinderCategory(distributorId, category.getCode());
        if (invOpt.isEmpty()) {
            return List.of();
        }

        List<InventoryMovement> movements = movementRepository.findByInventoryIdOrderByPerformedAtDesc(invOpt.get().getId());
        return movements.stream()
                .map(m -> {
                    InventoryMovementResponseDTO dto = new InventoryMovementResponseDTO();
                    dto.setId(m.getId());
                    dto.setInventoryId(invOpt.get().getId());
                    dto.setCylinderCategory(category.getCode());
                    dto.setMovementType(m.getMovementType());
                    dto.setQuantityChange(m.getQuantityChange());
                    dto.setReason(m.getReason());
                    dto.setReferenceNumber(m.getReferenceNumber());
                    dto.setPerformedBy(m.getPerformedBy());
                    dto.setPerformedAt(m.getPerformedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Aggregates real operational metrics for the Distributor Overview Dashboard.
     */
    public DistributorDashboardOverviewDTO getDashboardOverview(Long distributorId) {
        checkDistributorAccess(distributorId);

        Distributor distributor = distributorRepository.findById(distributorId)
                .orElseThrow(() -> new IllegalArgumentException("Distributor not found with ID: " + distributorId));

        DistributorDashboardOverviewDTO overview = new DistributorDashboardOverviewDTO();
        overview.setDistributorId(distributor.getId());
        overview.setDistributorName(distributor.getAgencyName());
        overview.setDistributorCode(distributor.getLicenceNumber());

        // Real bookings assigned to this distributor
        List<Booking> allBookings = bookingRepository.findByDistributorIdWithDetails(distributorId);
        LocalDateTime now = LocalDateTime.now();

        long newRequests = allBookings.stream().filter(b -> "PENDING".equalsIgnoreCase(b.getStatus())).count();
        long queuedOrders = allBookings.stream().filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus())).count();
        long cancelled = allBookings.stream().filter(b -> "CANCELLED".equalsIgnoreCase(b.getStatus())).count();

        overview.setNewBookingRequestsCount(newRequests);
        overview.setAcceptedQueuedOrdersCount(queuedOrders);
        overview.setCancelledBookingsCount(cancelled);

        // Real deliveries assigned to this distributor
        List<Delivery> allDeliveries = deliveryRepository.findByDistributorIdWithDetails(distributorId);
        long scheduled = allDeliveries.stream().filter(d -> "ASSIGNED".equalsIgnoreCase(d.getStatus())).count();
        long dispatched = allDeliveries.stream().filter(d -> "OUT_FOR_DELIVERY".equalsIgnoreCase(d.getStatus())).count();
        long completed = allDeliveries.stream().filter(d -> "DELIVERED".equalsIgnoreCase(d.getStatus())).count();

        overview.setScheduledDeliveriesCount(scheduled);
        overview.setDispatchedDeliveriesCount(dispatched);
        overview.setCompletedDeliveriesCount(completed);

        // Calculate queue age (age of CONFIRMED orders in hours) and overdue (> 168h / 7 days)
        List<Booking> queuedList = allBookings.stream()
                .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus()) && b.getBookingDate() != null)
                .toList();

        if (!queuedList.isEmpty()) {
            double totalHours = queuedList.stream()
                    .mapToDouble(b -> Math.max(0, Duration.between(b.getBookingDate(), now).toMinutes() / 60.0))
                    .sum();
            overview.setAverageQueueAgeHours(Math.round((totalHours / queuedList.size()) * 10.0) / 10.0);

            long overdue = queuedList.stream()
                    .filter(b -> Duration.between(b.getBookingDate(), now).toDays() >= 7)
                    .count();
            overview.setOverdueOrdersCount(overdue);
        } else {
            overview.setAverageQueueAgeHours(0.0);
            overview.setOverdueOrdersCount(0);
        }

        // Inventory snapshot
        List<CylinderInventoryResponseDTO> invDTOs = getDistributorInventory(distributorId);
        overview.setInventory(invDTOs);

        int totalFilled = invDTOs.stream().mapToInt(CylinderInventoryResponseDTO::getFilledStock).sum();
        int totalEmpty = invDTOs.stream().mapToInt(CylinderInventoryResponseDTO::getEmptyStock).sum();
        int totalReserved = invDTOs.stream().mapToInt(CylinderInventoryResponseDTO::getReservedStock).sum();
        int totalDamaged = invDTOs.stream().mapToInt(CylinderInventoryResponseDTO::getDamagedStock).sum();
        int totalAvailable = invDTOs.stream().mapToInt(CylinderInventoryResponseDTO::getAvailableStock).sum();

        overview.setTotalFilledStock(totalFilled);
        overview.setTotalEmptyStock(totalEmpty);
        overview.setTotalReservedStock(totalReserved);
        overview.setTotalDamagedStock(totalDamaged);
        overview.setTotalAvailableStock(totalAvailable);

        // Transparency / Allocation totals for current month
        LocalDate today = LocalDate.now();
        TransparencyReportDTO transparency = allocationService.getTransparencyReport(
                distributorId, today.getYear(), today.getMonthValue(), null);

        overview.setAllocations(transparency.getCategories());
        overview.setAllocatedMonthlyTotal(transparency.getTotalAllocated() != null ? transparency.getTotalAllocated() : 0);
        overview.setSoldMonthlyTotal(transparency.getTotalDelivered() != null ? transparency.getTotalDelivered() : 0);
        overview.setRemainingAllocationTotal(transparency.getTotalRemaining() != null ? transparency.getTotalRemaining() : 0);

        // Recent 5 bookings
        List<BookingResponseDTO> recentDTOs = allBookings.stream()
                .limit(5)
                .map(b -> new BookingResponseDTO(
                        b.getId(),
                        b.getCustomer() != null ? b.getCustomer().getId() : null,
                        b.getCustomer() != null ? b.getCustomer().getName() : "Customer",
                        distributor.getId(),
                        distributor.getAgencyName(),
                        b.getDeliveryAddress(),
                        b.getBookingDate(),
                        b.getQuantity(),
                        b.getStatus(),
                        b.getCylinderCategory(),
                        b.getCapacityKg()
                ))
                .collect(Collectors.toList());
        overview.setRecentBookings(recentDTOs);

        return overview;
    }

    private CylinderInventoryResponseDTO mapToDTO(CylinderInventory inv) {
        CylinderInventoryResponseDTO dto = new CylinderInventoryResponseDTO();
        dto.setId(inv.getId());
        dto.setDistributorId(inv.getDistributor() != null ? inv.getDistributor().getId() : null);
        dto.setDistributorName(inv.getDistributor() != null ? inv.getDistributor().getAgencyName() : null);

        CylinderCategory cat = CylinderCategory.fromCode(inv.getCylinderCategory());
        dto.setCylinderCategory(cat.getCode());
        dto.setCategoryDisplayName(cat.getDisplayName());
        dto.setCapacityKg(cat.getCapacityKg());

        dto.setFilledStock(inv.getFilledStock());
        dto.setEmptyStock(inv.getEmptyStock());
        dto.setReservedStock(inv.getReservedStock());
        dto.setDamagedStock(inv.getDamagedStock());
        dto.setAvailableStock(inv.getAvailableStock());

        dto.setLastAuditedAt(inv.getLastAuditedAt());
        dto.setLastAuditedBy(inv.getLastAuditedBy());
        return dto;
    }
}
