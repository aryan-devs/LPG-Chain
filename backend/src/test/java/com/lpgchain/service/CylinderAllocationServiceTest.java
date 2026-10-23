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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CylinderAllocationServiceTest {

    @Mock
    private CylinderAllocationRepository allocationRepository;

    @Mock
    private AllocationAdjustmentRepository adjustmentRepository;

    @Mock
    private DistributorRepository distributorRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private AllocationAuthorizationService authorizationService;

    @InjectMocks
    private CylinderAllocationService allocationService;

    private Distributor distributor;
    private CylinderAllocation allocationDomestic;
    private User adminUser;

    @BeforeEach
    void setUp() {
        distributor = new Distributor(
                1L, "Metro Gas Agency", "LIC-001", "+919876543210",
                "10 MG Road", "ACTIVE", "secretPasskey",
                LocalDateTime.now().plusDays(2), false,
                "400001", "Mumbai", "Maharashtra",
                18.9220, 72.8347, "BOTH", "09:00 - 18:00"
        );

        adminUser = new User();
        adminUser.setId(10L);
        adminUser.setEmail("admin@lpgchain.com");

        allocationDomestic = new CylinderAllocation(
                distributor,
                "DOMESTIC_14_2",
                14.2,
                500,
                2026,
                10,
                LocalDate.of(2026, 10, 1),
                "IOCL-REF-2026-10-01",
                "admin@lpgchain.com"
        );
        allocationDomestic.setId(100L);
    }

    @Test
    @DisplayName("createAllocation: Successfully saves official allocation when parameters valid")
    void createAllocation_Success() {
        CylinderAllocationRequestDTO dto = new CylinderAllocationRequestDTO();
        dto.setDistributorId(1L);
        dto.setCylinderCategory("DOMESTIC_14_2");
        dto.setAllocatedQuantity(500);
        dto.setReportingYear(2026);
        dto.setReportingMonth(10);
        dto.setSourceReference("OMC-QUOTA-OCT");

        when(distributorRepository.findById(1L)).thenReturn(Optional.of(distributor));
        when(allocationRepository.findByDistributorIdAndCylinderCategoryAndReportingYearAndReportingMonth(
                1L, "DOMESTIC_14_2", 2026, 10)).thenReturn(Optional.empty());
        when(authorizationService.getAuthenticatedUser()).thenReturn(adminUser);
        when(allocationRepository.save(any(CylinderAllocation.class))).thenAnswer(inv -> {
            CylinderAllocation a = inv.getArgument(0);
            a.setId(101L);
            return a;
        });

        CylinderAllocationResponseDTO response = allocationService.createAllocation(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getDistributorId()).isEqualTo(1L);
        assertThat(response.getCylinderCategory()).isEqualTo("DOMESTIC_14_2");
        assertThat(response.getAllocatedQuantity()).isEqualTo(500);
        assertThat(response.getReportingPeriod()).isEqualTo("2026-10");
        assertThat(response.getCreatedBy()).isEqualTo("admin@lpgchain.com");

        verify(authorizationService).checkCanManageAllocations();
        verify(allocationRepository).save(any(CylinderAllocation.class));
    }

    @Test
    @DisplayName("createAllocation: Throws IllegalArgumentException on duplicate allocation for same period/category")
    void createAllocation_DuplicateRejected() {
        CylinderAllocationRequestDTO dto = new CylinderAllocationRequestDTO();
        dto.setDistributorId(1L);
        dto.setCylinderCategory("DOMESTIC_14_2");
        dto.setAllocatedQuantity(300);
        dto.setReportingYear(2026);
        dto.setReportingMonth(10);

        when(distributorRepository.findById(1L)).thenReturn(Optional.of(distributor));
        when(allocationRepository.findByDistributorIdAndCylinderCategoryAndReportingYearAndReportingMonth(
                1L, "DOMESTIC_14_2", 2026, 10)).thenReturn(Optional.of(allocationDomestic));

        assertThatThrownBy(() -> allocationService.createAllocation(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Allocation record already exists");
    }

    @Test
    @DisplayName("createAllocation: Throws IllegalArgumentException when quantity is negative")
    void createAllocation_NegativeQuantityRejected() {
        CylinderAllocationRequestDTO dto = new CylinderAllocationRequestDTO();
        dto.setDistributorId(1L);
        dto.setCylinderCategory("DOMESTIC_14_2");
        dto.setAllocatedQuantity(-10);
        dto.setReportingYear(2026);
        dto.setReportingMonth(10);

        assertThatThrownBy(() -> allocationService.createAllocation(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allocatedQuantity must be non-negative");
    }

    @Test
    @DisplayName("updateAllocation: Successfully updates quantity and sourceReference")
    void updateAllocation_Success() {
        when(allocationRepository.findById(100L)).thenReturn(Optional.of(allocationDomestic));
        when(authorizationService.getAuthenticatedUser()).thenReturn(adminUser);
        when(adjustmentRepository.findByAllocationIdOrderByAdjustedAtDesc(100L)).thenReturn(Collections.emptyList());
        when(allocationRepository.save(any(CylinderAllocation.class))).thenAnswer(inv -> inv.getArgument(0));

        CylinderAllocationResponseDTO response = allocationService.updateAllocation(100L, 650, "UPDATED-REF-999");

        assertThat(response.getAllocatedQuantity()).isEqualTo(650);
        assertThat(response.getSourceReference()).isEqualTo("UPDATED-REF-999");
        assertThat(response.getUpdatedBy()).isEqualTo("admin@lpgchain.com");
    }

    @Test
    @DisplayName("addAdjustment: Records auditable quota adjustment and updates allocation metadata")
    void addAdjustment_Success() {
        AllocationAdjustmentRequestDTO dto = new AllocationAdjustmentRequestDTO();
        dto.setAdjustmentType("EMERGENCY_QUOTA");
        dto.setAdjustedQuantity(50);
        dto.setReason("Festival quota boost approved by OMC");
        dto.setReferenceNumber("BOOST-102");

        when(allocationRepository.findById(100L)).thenReturn(Optional.of(allocationDomestic));
        when(authorizationService.getAuthenticatedUser()).thenReturn(adminUser);
        when(adjustmentRepository.save(any(AllocationAdjustment.class))).thenAnswer(inv -> {
            AllocationAdjustment adj = inv.getArgument(0);
            adj.setId(201L);
            return adj;
        });

        AllocationAdjustmentResponseDTO response = allocationService.addAdjustment(100L, dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(201L);
        assertThat(response.getAdjustedQuantity()).isEqualTo(50);
        assertThat(response.getAdjustmentType()).isEqualTo("EMERGENCY_QUOTA");
        assertThat(response.getReason()).isEqualTo("Festival quota boost approved by OMC");
        assertThat(response.getAdjustedBy()).isEqualTo("admin@lpgchain.com");

        verify(authorizationService).checkCanManageAllocations();
        verify(adjustmentRepository).save(any(AllocationAdjustment.class));
        verify(allocationRepository).save(allocationDomestic);
    }

    @Test
    @DisplayName("addAdjustment: Throws IllegalArgumentException on blank reason or zero quantity")
    void addAdjustment_ValidationFailure() {
        AllocationAdjustmentRequestDTO dto = new AllocationAdjustmentRequestDTO();
        dto.setAdjustedQuantity(0);
        dto.setReason("Some reason");

        when(allocationRepository.findById(100L)).thenReturn(Optional.of(allocationDomestic));

        assertThatThrownBy(() -> allocationService.addAdjustment(100L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adjustedQuantity cannot be null or zero");

        dto.setAdjustedQuantity(20);
        dto.setReason("   ");

        assertThatThrownBy(() -> allocationService.addAdjustment(100L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason is required");
    }

    @Test
    @DisplayName("getTransparencyReport: Computes accurate allocated, delivered, and remaining figures using delivered bookings only")
    void getTransparencyReport_CalculatesDeliveredOnly() {
        // Setup allocation: 500 allocated
        when(distributorRepository.findById(1L)).thenReturn(Optional.of(distributor));
        when(allocationRepository.findByDistributorIdAndReportingYearAndReportingMonth(1L, 2026, 10))
                .thenReturn(List.of(allocationDomestic));

        // Adjustments: +20
        AllocationAdjustment adj = new AllocationAdjustment(
                allocationDomestic, "CORRECTION", 20, "Reconciliation correction", "REF-1", "admin@lpgchain.com");
        when(adjustmentRepository.findByAllocationIdOrderByAdjustedAtDesc(100L)).thenReturn(List.of(adj));

        // Bookings in database:
        // Booking 1: DELIVERED, Domestic 14.2, qty=2, delivered Oct 5 2026
        Booking b1 = new Booking();
        b1.setId(1001L);
        b1.setDistributor(distributor);
        b1.setStatus("DELIVERED");
        b1.setCylinderCategory("DOMESTIC_14_2");
        b1.setQuantity(2);
        b1.setBookingDate(LocalDateTime.of(2026, 10, 5, 10, 0));

        // Booking 2: DELIVERED, Domestic 14.2, qty=3, delivered Oct 10 2026 (via delivery record)
        Booking b2 = new Booking();
        b2.setId(1002L);
        b2.setDistributor(distributor);
        b2.setStatus("DELIVERED");
        b2.setCylinderCategory("DOMESTIC_14_2");
        b2.setQuantity(3);
        b2.setBookingDate(LocalDateTime.of(2026, 9, 28, 10, 0)); // booked late September, but delivered in October!

        Delivery d2 = new Delivery();
        d2.setId(5002L);
        d2.setBooking(b2);
        d2.setDistributor(distributor);
        d2.setStatus("DELIVERED");
        d2.setDeliveredAt(LocalDateTime.of(2026, 10, 10, 14, 30));

        // Booking 3: PENDING, should NOT be counted as delivered
        Booking b3 = new Booking();
        b3.setId(1003L);
        b3.setDistributor(distributor);
        b3.setStatus("PENDING");
        b3.setCylinderCategory("DOMESTIC_14_2");
        b3.setQuantity(1);
        b3.setBookingDate(LocalDateTime.of(2026, 10, 6, 11, 0));

        // Booking 4: CANCELLED, should NOT be counted
        Booking b4 = new Booking();
        b4.setId(1004L);
        b4.setDistributor(distributor);
        b4.setStatus("CANCELLED");
        b4.setCylinderCategory("DOMESTIC_14_2");
        b4.setQuantity(1);

        // Booking 5: DELIVERED in September, outside target October period
        Booking b5 = new Booking();
        b5.setId(1005L);
        b5.setDistributor(distributor);
        b5.setStatus("DELIVERED");
        b5.setCylinderCategory("DOMESTIC_14_2");
        b5.setQuantity(5);
        b5.setBookingDate(LocalDateTime.of(2026, 9, 15, 10, 0));

        // Repository returns completed bookings (status == 'DELIVERED')
        when(bookingRepository.findByDistributorIdAndStatus(1L, "DELIVERED"))
                .thenReturn(List.of(b1, b2, b5));
        when(deliveryRepository.findByDistributorId(1L))
                .thenReturn(List.of(d2));

        TransparencyReportDTO report = allocationService.getTransparencyReport(1L, 2026, 10, null);

        assertThat(report).isNotNull();
        assertThat(report.getDistributorId()).isEqualTo(1L);
        assertThat(report.getReportingPeriod()).isEqualTo("2026-10");
        assertThat(report.getCategories()).hasSize(3);

        // Domestic 14.2 Category check
        CategoryTransparencyDTO domesticReport = report.getCategories().stream()
                .filter(c -> "DOMESTIC_14_2".equals(c.getCylinderCategory()))
                .findFirst()
                .orElseThrow();

        assertThat(domesticReport.isAllocated()).isTrue();
        assertThat(domesticReport.getAllocatedQuantity()).isEqualTo(500);
        assertThat(domesticReport.getAdjustmentQuantity()).isEqualTo(20);
        assertThat(domesticReport.getNetAllocatedQuantity()).isEqualTo(520);
        // Delivered should be b1 (2) + b2 (3) = 5 cylinders (b5 was September, b3 pending, b4 cancelled)
        assertThat(domesticReport.getDeliveredQuantity()).isEqualTo(5);
        // Remaining should be 520 - 5 = 515
        assertThat(domesticReport.getRemainingAllocation()).isEqualTo(515);
        assertThat(domesticReport.getUtilizationPercentage()).isEqualTo(0.96); // (5 / 520) * 100 = 0.96%

        // Commercial 19.0 Category check (not allocated)
        CategoryTransparencyDTO commercialReport = report.getCategories().stream()
                .filter(c -> "COMMERCIAL_19_0".equals(c.getCylinderCategory()))
                .findFirst()
                .orElseThrow();

        assertThat(commercialReport.isAllocated()).isFalse();
        assertThat(commercialReport.getAllocatedQuantity()).isNull();
        assertThat(commercialReport.getRemainingAllocation()).isNull();
        assertThat(commercialReport.getStatusMessage()).contains("No official cylinder allocation has been published");
    }

    @Test
    @DisplayName("getTransparencyReport: Prevents double counting duplicate bookings")
    void getTransparencyReport_PreventsDoubleCounting() {
        when(distributorRepository.findById(1L)).thenReturn(Optional.of(distributor));
        when(allocationRepository.findByDistributorIdAndReportingYearAndReportingMonth(1L, 2026, 10))
                .thenReturn(List.of(allocationDomestic));
        when(adjustmentRepository.findByAllocationIdOrderByAdjustedAtDesc(100L)).thenReturn(Collections.emptyList());

        Booking duplicateBooking = new Booking();
        duplicateBooking.setId(2001L);
        duplicateBooking.setDistributor(distributor);
        duplicateBooking.setStatus("DELIVERED");
        duplicateBooking.setCylinderCategory("DOMESTIC_14_2");
        duplicateBooking.setQuantity(1);
        duplicateBooking.setBookingDate(LocalDateTime.of(2026, 10, 8, 12, 0));

        // Same booking listed twice in list
        when(bookingRepository.findByDistributorIdAndStatus(1L, "DELIVERED"))
                .thenReturn(List.of(duplicateBooking, duplicateBooking));
        when(deliveryRepository.findByDistributorId(1L))
                .thenReturn(Collections.emptyList());

        TransparencyReportDTO report = allocationService.getTransparencyReport(1L, 2026, 10, "DOMESTIC_14_2");

        CategoryTransparencyDTO cat = report.getCategories().get(0);
        assertThat(cat.getDeliveredQuantity()).isEqualTo(1); // Not 2!
        assertThat(cat.getRemainingAllocation()).isEqualTo(499);
    }
}
