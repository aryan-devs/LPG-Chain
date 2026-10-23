package com.lpgchain.service;

import com.lpgchain.dto.AllocationAdjustmentRequestDTO;
import com.lpgchain.dto.AllocationAdjustmentResponseDTO;
import com.lpgchain.dto.CategoryTransparencyDTO;
import com.lpgchain.dto.CylinderAllocationRequestDTO;
import com.lpgchain.dto.CylinderAllocationResponseDTO;
import com.lpgchain.dto.TransparencyReportDTO;
import com.lpgchain.entity.AllocationAdjustment;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.CylinderAllocation;
import com.lpgchain.entity.CylinderCategory;
import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.User;
import com.lpgchain.repository.AllocationAdjustmentRepository;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.CylinderAllocationRepository;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.DistributorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CylinderAllocationService — Core service for managing official cylinder allocations,
 * auditable quota adjustments, and calculating sales transparency figures against real database transactions.
 */
@Service
@Transactional(readOnly = true)
public class CylinderAllocationService {

    private final CylinderAllocationRepository allocationRepository;
    private final AllocationAdjustmentRepository adjustmentRepository;
    private final DistributorRepository distributorRepository;
    private final BookingRepository bookingRepository;
    private final DeliveryRepository deliveryRepository;
    private final AllocationAuthorizationService authorizationService;

    public CylinderAllocationService(CylinderAllocationRepository allocationRepository,
                                    AllocationAdjustmentRepository adjustmentRepository,
                                    DistributorRepository distributorRepository,
                                    BookingRepository bookingRepository,
                                    DeliveryRepository deliveryRepository,
                                    AllocationAuthorizationService authorizationService) {
        this.allocationRepository = allocationRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.distributorRepository = distributorRepository;
        this.bookingRepository = bookingRepository;
        this.deliveryRepository = deliveryRepository;
        this.authorizationService = authorizationService;
    }

    /**
     * Registers a new official cylinder allocation for a distributor, category, and period.
     * Enforces ADMIN authorization, non-negative quantities, and prevents duplicate allocations.
     */
    @Transactional
    public CylinderAllocationResponseDTO createAllocation(CylinderAllocationRequestDTO dto) {
        authorizationService.checkCanManageAllocations();

        if (dto.getDistributorId() == null) {
            throw new IllegalArgumentException("distributorId is required");
        }
        if (dto.getAllocatedQuantity() == null || dto.getAllocatedQuantity() < 0) {
            throw new IllegalArgumentException("allocatedQuantity must be non-negative");
        }
        if (dto.getReportingYear() == null || dto.getReportingYear() < 2020) {
            throw new IllegalArgumentException("reportingYear must be 2020 or later");
        }
        if (dto.getReportingMonth() == null || dto.getReportingMonth() < 1 || dto.getReportingMonth() > 12) {
            throw new IllegalArgumentException("reportingMonth must be between 1 and 12");
        }

        Distributor distributor = distributorRepository.findById(dto.getDistributorId())
                .orElseThrow(() -> new IllegalArgumentException("Distributor not found with ID: " + dto.getDistributorId()));

        CylinderCategory category = CylinderCategory.fromCode(dto.getCylinderCategory());

        // Prevent duplicate or conflicting allocation for the same distributor, category, and month/year
        Optional<CylinderAllocation> existing = allocationRepository
                .findByDistributorIdAndCylinderCategoryAndReportingYearAndReportingMonth(
                        distributor.getId(), category.getCode(), dto.getReportingYear(), dto.getReportingMonth());

        if (existing.isPresent()) {
            throw new IllegalArgumentException(String.format(
                    "Allocation record already exists for distributor ID %d, category %s, and period %04d-%02d",
                    distributor.getId(), category.getCode(), dto.getReportingYear(), dto.getReportingMonth()));
        }

        User currentUser = authorizationService.getAuthenticatedUser();
        String createdBy = currentUser != null ? currentUser.getEmail() : "system";

        Double capacityKg = dto.getCapacityKg() != null ? dto.getCapacityKg() : category.getCapacityKg();
        LocalDate allocDate = dto.getAllocationDate() != null ? dto.getAllocationDate() : LocalDate.now();

        CylinderAllocation allocation = new CylinderAllocation(
                distributor,
                category.getCode(),
                capacityKg,
                dto.getAllocatedQuantity(),
                dto.getReportingYear(),
                dto.getReportingMonth(),
                allocDate,
                dto.getSourceReference(),
                createdBy
        );

        CylinderAllocation saved = allocationRepository.save(allocation);
        return mapToResponseDTO(saved, 0);
    }

    /**
     * Updates an existing cylinder allocation record with audit logging.
     * Enforces ADMIN authorization and non-negative quantities.
     */
    @Transactional
    public CylinderAllocationResponseDTO updateAllocation(Long id, Integer quantity, String sourceReference) {
        authorizationService.checkCanManageAllocations();

        CylinderAllocation allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found with ID: " + id));

        if (quantity != null) {
            if (quantity < 0) {
                throw new IllegalArgumentException("allocatedQuantity must be non-negative");
            }
            allocation.setAllocatedQuantity(quantity);
        }

        if (sourceReference != null) {
            allocation.setSourceReference(sourceReference);
        }

        User currentUser = authorizationService.getAuthenticatedUser();
        allocation.setUpdatedBy(currentUser != null ? currentUser.getEmail() : "system");
        allocation.setUpdatedAt(LocalDateTime.now());

        CylinderAllocation saved = allocationRepository.save(allocation);
        int adjustments = calculateTotalAdjustments(saved.getId());
        return mapToResponseDTO(saved, adjustments);
    }

    /**
     * Records an auditable quota adjustment (e.g. return, emergency reallocation, correction) against an allocation.
     * Enforces ADMIN authorization.
     */
    @Transactional
    public AllocationAdjustmentResponseDTO addAdjustment(Long allocationId, AllocationAdjustmentRequestDTO dto) {
        authorizationService.checkCanManageAllocations();

        CylinderAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found with ID: " + allocationId));

        if (dto.getAdjustedQuantity() == null || dto.getAdjustedQuantity() == 0) {
            throw new IllegalArgumentException("adjustedQuantity cannot be null or zero");
        }
        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("reason is required for auditable adjustments");
        }

        User currentUser = authorizationService.getAuthenticatedUser();
        String adjustedBy = currentUser != null ? currentUser.getEmail() : "system";

        AllocationAdjustment adjustment = new AllocationAdjustment(
                allocation,
                dto.getAdjustmentType() != null ? dto.getAdjustmentType().trim().toUpperCase() : "CORRECTION",
                dto.getAdjustedQuantity(),
                dto.getReason().trim(),
                dto.getReferenceNumber(),
                adjustedBy
        );

        AllocationAdjustment saved = adjustmentRepository.save(adjustment);

        // Update the allocation's last modified metadata
        allocation.setUpdatedAt(LocalDateTime.now());
        allocation.setUpdatedBy(adjustedBy);
        allocationRepository.save(allocation);

        return mapToAdjustmentResponseDTO(saved);
    }

    /**
     * Retrieves all auditable adjustments for an allocation record.
     * Accessible to ADMIN or active members of the associated distributor.
     */
    public List<AllocationAdjustmentResponseDTO> getAdjustmentsForAllocation(Long allocationId) {
        CylinderAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found with ID: " + allocationId));

        authorizationService.checkCanViewDistributorAllocations(allocation.getDistributor().getId());

        return adjustmentRepository.findByAllocationIdOrderByAdjustedAtDesc(allocationId).stream()
                .map(this::mapToAdjustmentResponseDTO)
                .toList();
    }

    /**
     * Retrieves all allocation records for a distributor (optionally filtered by year and month).
     * Accessible to ADMIN or active members of that distributor.
     */
    public List<CylinderAllocationResponseDTO> getDistributorAllocations(Long distributorId, Integer year, Integer month) {
        authorizationService.checkCanViewDistributorAllocations(distributorId);

        List<CylinderAllocation> allocations;
        if (year != null && month != null) {
            allocations = allocationRepository.findByDistributorIdAndReportingYearAndReportingMonth(distributorId, year, month);
        } else {
            allocations = allocationRepository.findByDistributorId(distributorId);
        }

        return allocations.stream()
                .map(alloc -> {
                    int adjustments = calculateTotalAdjustments(alloc.getId());
                    return mapToResponseDTO(alloc, adjustments);
                })
                .sorted(Comparator.comparing(CylinderAllocationResponseDTO::getReportingPeriod).reversed()
                        .thenComparing(CylinderAllocationResponseDTO::getCylinderCategory))
                .toList();
    }

    /**
     * Retrieves an allocation record by ID with authorization checks.
     */
    public CylinderAllocationResponseDTO getAllocationById(Long id) {
        CylinderAllocation alloc = allocationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found with ID: " + id));

        authorizationService.checkCanViewDistributorAllocations(alloc.getDistributor().getId());

        int adjustments = calculateTotalAdjustments(alloc.getId());
        return mapToResponseDTO(alloc, adjustments);
    }

    /**
     * Generates the comprehensive, transparent allocation and completed sales report for a distributor and period.
     * Public / customer safe: never exposes customer PII or distributor credentials.
     *
     * Rules:
     * - Only counts bookings where status == 'DELIVERED'.
     * - Uses actual completion timestamps to attribute sales to reporting periods.
     * - Each booking is counted exactly once by distinct booking ID.
     * - Never invents allocations or deliveries when not present in the database.
     */
    public TransparencyReportDTO getTransparencyReport(Long distributorId, Integer year, Integer month, String categoryFilter) {
        Distributor distributor = distributorRepository.findById(distributorId)
                .orElseThrow(() -> new IllegalArgumentException("Distributor not found with ID: " + distributorId));

        // Default to current calendar year and month if not provided
        int reportYear = year != null ? year : LocalDate.now().getYear();
        int reportMonth = month != null ? month : LocalDate.now().getMonthValue();
        String periodCode = String.format("%04d-%02d", reportYear, reportMonth);

        // Date bounds for the reporting period
        YearMonth ym = YearMonth.of(reportYear, reportMonth);
        LocalDateTime startOfMonth = ym.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = ym.atEndOfMonth().atTime(23, 59, 59, 999999999);

        // Determine categories to report
        List<CylinderCategory> targetCategories = new ArrayList<>();
        if (categoryFilter != null && !categoryFilter.isBlank()) {
            targetCategories.add(CylinderCategory.fromCode(categoryFilter));
        } else {
            targetCategories.addAll(List.of(
                    CylinderCategory.DOMESTIC_14_2,
                    CylinderCategory.DOMESTIC_5_0,
                    CylinderCategory.COMMERCIAL_19_0
            ));
        }

        // Fetch official allocations for this distributor in this period
        List<CylinderAllocation> periodAllocations = allocationRepository
                .findByDistributorIdAndReportingYearAndReportingMonth(distributorId, reportYear, reportMonth);

        Map<String, CylinderAllocation> allocationMap = periodAllocations.stream()
                .collect(Collectors.toMap(CylinderAllocation::getCylinderCategory, a -> a, (a, b) -> a));

        // Fetch completed bookings for this distributor
        List<Booking> completedBookings = bookingRepository.findByDistributorIdAndStatus(distributorId, "DELIVERED");

        // Map delivery records to look up completion timestamps if deliveries were assigned
        List<Delivery> distributorDeliveries = deliveryRepository.findByDistributorId(distributorId);
        Map<Long, Delivery> bookingDeliveryMap = distributorDeliveries.stream()
                .filter(d -> d.getBooking() != null)
                .collect(Collectors.toMap(d -> d.getBooking().getId(), d -> d, (d1, d2) -> d1));

        // Aggregate completed sales by category for the target period
        Map<String, Integer> deliveredQuantitiesByCategory = targetCategories.stream()
                .collect(Collectors.toMap(CylinderCategory::getCode, c -> 0));

        Set<Long> processedBookingIds = new HashSet<>();
        LocalDateTime mostRecentActivity = null;

        for (Booking b : completedBookings) {
            if (b.getId() == null || processedBookingIds.contains(b.getId())) {
                continue; // Prevent double-counting
            }
            processedBookingIds.add(b.getId());

            // Determine effective completion date
            LocalDateTime completionDate = null;
            Delivery d = bookingDeliveryMap.get(b.getId());
            if (d != null && "DELIVERED".equalsIgnoreCase(d.getStatus()) && d.getDeliveredAt() != null) {
                completionDate = d.getDeliveredAt();
            } else if (b.getBookingDate() != null) {
                completionDate = b.getBookingDate();
            }

            if (completionDate == null || completionDate.isBefore(startOfMonth) || completionDate.isAfter(endOfMonth)) {
                continue; // Not within target reporting month
            }

            // Determine category
            CylinderCategory bCat = b.getCylinderCategory() != null
                    ? CylinderCategory.fromCode(b.getCylinderCategory())
                    : CylinderCategory.DOMESTIC_14_2;

            int qty = b.getQuantity() != null && b.getQuantity() > 0 ? b.getQuantity() : 1;
            deliveredQuantitiesByCategory.merge(bCat.getCode(), qty, Integer::sum);

            if (mostRecentActivity == null || completionDate.isAfter(mostRecentActivity)) {
                mostRecentActivity = completionDate;
            }
        }

        // Build category breakdown
        List<CategoryTransparencyDTO> categoryDTOs = new ArrayList<>();
        int totalAllocated = 0;
        int totalNetAllocated = 0;
        int totalDelivered = 0;
        int totalAdjustments = 0;
        boolean hasAnyAllocation = false;

        for (CylinderCategory cat : targetCategories) {
            CategoryTransparencyDTO catDTO = new CategoryTransparencyDTO();
            catDTO.setCylinderCategory(cat.getCode());
            catDTO.setCategoryDisplayName(cat.getDisplayName());
            catDTO.setCapacityKg(cat.getCapacityKg());

            int delivered = deliveredQuantitiesByCategory.getOrDefault(cat.getCode(), 0);
            catDTO.setDeliveredQuantity(delivered);
            totalDelivered += delivered;

            CylinderAllocation alloc = allocationMap.get(cat.getCode());
            if (alloc != null) {
                hasAnyAllocation = true;
                catDTO.setAllocated(true);
                catDTO.setAllocatedQuantity(alloc.getAllocatedQuantity());
                totalAllocated += alloc.getAllocatedQuantity();

                int adj = calculateTotalAdjustments(alloc.getId());
                catDTO.setAdjustmentQuantity(adj);
                totalAdjustments += adj;

                int netAlloc = alloc.getAllocatedQuantity() + adj;
                catDTO.setNetAllocatedQuantity(netAlloc);
                totalNetAllocated += netAlloc;

                int remaining = netAlloc - delivered;
                catDTO.setRemainingAllocation(remaining);

                double utilRate = netAlloc > 0
                        ? Math.round(((double) delivered / netAlloc) * 10000.0) / 100.0
                        : 0.0;
                catDTO.setUtilizationPercentage(utilRate);

                catDTO.setSourceReference(alloc.getSourceReference());
                catDTO.setLastUpdated(alloc.getUpdatedAt() != null ? alloc.getUpdatedAt() : alloc.getCreatedAt());
                catDTO.setStatusMessage("Verified active quota published by OMC");

                if (mostRecentActivity == null || (alloc.getUpdatedAt() != null && alloc.getUpdatedAt().isAfter(mostRecentActivity))) {
                    mostRecentActivity = alloc.getUpdatedAt() != null ? alloc.getUpdatedAt() : alloc.getCreatedAt();
                }
            } else {
                catDTO.setAllocated(false);
                catDTO.setAllocatedQuantity(null);
                catDTO.setAdjustmentQuantity(0);
                catDTO.setNetAllocatedQuantity(null);
                catDTO.setRemainingAllocation(null);
                catDTO.setUtilizationPercentage(null);
                catDTO.setSourceReference(null);
                catDTO.setLastUpdated(null);
                catDTO.setStatusMessage("No official cylinder allocation has been published for this category and reporting period.");
            }

            categoryDTOs.add(catDTO);
        }

        TransparencyReportDTO report = new TransparencyReportDTO();
        report.setDistributorId(distributor.getId());
        report.setDistributorName(distributor.getAgencyName());
        report.setLocation((distributor.getCity() != null ? distributor.getCity() : "") +
                (distributor.getState() != null ? ", " + distributor.getState() : ""));
        report.setReportingYear(reportYear);
        report.setReportingMonth(reportMonth);
        report.setReportingPeriod(periodCode);
        report.setCategories(categoryDTOs);
        report.setHasAllocations(hasAnyAllocation);
        report.setTotalAllocated(totalAllocated);
        report.setTotalAdjustments(totalAdjustments);
        report.setTotalDelivered(totalDelivered);
        report.setTotalRemaining(hasAnyAllocation ? (totalNetAllocated - totalDelivered) : 0);

        double overallUtil = totalNetAllocated > 0
                ? Math.round(((double) totalDelivered / totalNetAllocated) * 10000.0) / 100.0
                : 0.0;
        report.setOverallUtilizationPercentage(overallUtil);
        report.setLastUpdated(mostRecentActivity != null ? mostRecentActivity : LocalDateTime.now());

        return report;
    }

    /**
     * Lists all historical reporting periods with published allocations, plus the current month.
     */
    public List<String> getAvailableReportingPeriods() {
        List<String> distinctPeriods = new ArrayList<>(allocationRepository.findDistinctReportingPeriods());
        String currentPeriod = String.format("%04d-%02d", LocalDate.now().getYear(), LocalDate.now().getMonthValue());
        if (!distinctPeriods.contains(currentPeriod)) {
            distinctPeriods.add(0, currentPeriod);
        }
        distinctPeriods.sort(Comparator.reverseOrder());
        return distinctPeriods;
    }

    // --- Private Helper Methods ---

    private int calculateTotalAdjustments(Long allocationId) {
        return adjustmentRepository.findByAllocationIdOrderByAdjustedAtDesc(allocationId).stream()
                .mapToInt(AllocationAdjustment::getAdjustedQuantity)
                .sum();
    }

    private CylinderAllocationResponseDTO mapToResponseDTO(CylinderAllocation alloc, int adjustmentQuantity) {
        CylinderAllocationResponseDTO dto = new CylinderAllocationResponseDTO();
        dto.setId(alloc.getId());
        dto.setDistributorId(alloc.getDistributor() != null ? alloc.getDistributor().getId() : null);
        dto.setDistributorName(alloc.getDistributor() != null ? alloc.getDistributor().getAgencyName() : null);
        dto.setCylinderCategory(alloc.getCylinderCategory());
        dto.setCapacityKg(alloc.getCapacityKg());

        CylinderCategory cat = CylinderCategory.fromCode(alloc.getCylinderCategory());
        dto.setCategoryDisplayName(cat.getDisplayName());

        dto.setAllocatedQuantity(alloc.getAllocatedQuantity());
        dto.setAdjustmentQuantity(adjustmentQuantity);
        dto.setNetAllocatedQuantity(alloc.getAllocatedQuantity() + adjustmentQuantity);
        dto.setReportingYear(alloc.getReportingYear());
        dto.setReportingMonth(alloc.getReportingMonth());
        dto.setReportingPeriod(alloc.getReportingPeriod());
        dto.setAllocationDate(alloc.getAllocationDate());
        dto.setSourceReference(alloc.getSourceReference());
        dto.setCreatedAt(alloc.getCreatedAt());
        dto.setUpdatedAt(alloc.getUpdatedAt());
        dto.setCreatedBy(alloc.getCreatedBy());
        dto.setUpdatedBy(alloc.getUpdatedBy());
        return dto;
    }

    private AllocationAdjustmentResponseDTO mapToAdjustmentResponseDTO(AllocationAdjustment adj) {
        AllocationAdjustmentResponseDTO dto = new AllocationAdjustmentResponseDTO();
        dto.setId(adj.getId());
        dto.setAllocationId(adj.getAllocation() != null ? adj.getAllocation().getId() : null);
        dto.setAdjustmentType(adj.getAdjustmentType());
        dto.setAdjustedQuantity(adj.getAdjustedQuantity());
        dto.setReason(adj.getReason());
        dto.setReferenceNumber(adj.getReferenceNumber());
        dto.setAdjustedBy(adj.getAdjustedBy());
        dto.setAdjustedAt(adj.getAdjustedAt());
        return dto;
    }
}
