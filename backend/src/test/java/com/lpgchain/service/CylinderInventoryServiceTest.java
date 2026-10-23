package com.lpgchain.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CylinderInventoryServiceTest {

    @Mock
    private CylinderInventoryRepository inventoryRepository;

    @Mock
    private InventoryMovementRepository movementRepository;

    @Mock
    private DistributorRepository distributorRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private CylinderAllocationService allocationService;

    @Mock
    private BookingAuthorizationService authorizationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CylinderInventoryService inventoryService;

    private Distributor distributor;
    private User testUser;

    @BeforeEach
    void setUp() {
        distributor = new Distributor();
        distributor.setId(10L);
        distributor.setAgencyName("Apex Gas Agency");
        distributor.setLicenceNumber("DIST-APEX-10");

        testUser = new User();
        testUser.setId(5L);
        testUser.setEmail("operator@apexgas.com");
    }

    @Test
    @DisplayName("getDistributorInventory: initializes categories and calculates reserved and available stock")
    void getDistributorInventory_success() {
        when(authorizationService.getAuthentication()).thenReturn(authentication);
        when(authorizationService.isAdmin(authentication)).thenReturn(false);
        when(authorizationService.getAuthenticatedUser()).thenReturn(testUser);
        when(authorizationService.isDistributorMember(10L, testUser)).thenReturn(true);
        when(distributorRepository.findById(10L)).thenReturn(Optional.of(distributor));

        CylinderInventory existingInv = new CylinderInventory(distributor, "DOMESTIC_14_2", 14.2, 100, 20, 0, 2, "test");
        List<CylinderInventory> currentList = new ArrayList<>(List.of(existingInv));
        when(inventoryRepository.findByDistributorId(10L)).thenReturn(currentList);

        when(inventoryRepository.save(any(CylinderInventory.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking confirmedBooking = new Booking();
        confirmedBooking.setId(101L);
        confirmedBooking.setDistributor(distributor);
        confirmedBooking.setCylinderCategory("DOMESTIC_14_2");
        confirmedBooking.setQuantity(5);
        confirmedBooking.setStatus("CONFIRMED");

        when(bookingRepository.findByDistributorIdAndStatus(10L, "CONFIRMED")).thenReturn(List.of(confirmedBooking));

        List<CylinderInventoryResponseDTO> result = inventoryService.getDistributorInventory(10L);

        assertThat(result).hasSize(3); // DOMESTIC_14_2, DOMESTIC_5_0, COMMERCIAL_19_0
        CylinderInventoryResponseDTO d14 = result.stream()
                .filter(d -> "DOMESTIC_14_2".equals(d.getCylinderCategory()))
                .findFirst().orElseThrow();
        assertThat(d14.getFilledStock()).isEqualTo(100);
        assertThat(d14.getReservedStock()).isEqualTo(5);
        assertThat(d14.getAvailableStock()).isEqualTo(95);
    }

    @Test
    @DisplayName("getDistributorInventory: throws AccessDeniedException when user is not member of distributor")
    void getDistributorInventory_accessDenied() {
        when(authorizationService.getAuthentication()).thenReturn(authentication);
        when(authorizationService.isAdmin(authentication)).thenReturn(false);
        when(authorizationService.getAuthenticatedUser()).thenReturn(testUser);
        when(authorizationService.isDistributorMember(10L, testUser)).thenReturn(false);

        assertThatThrownBy(() -> inventoryService.getDistributorInventory(10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Forbidden");
    }

    @Test
    @DisplayName("updateStock: updates physical counts, records auditable movement, and returns updated DTO")
    void updateStock_success() {
        when(authorizationService.getAuthentication()).thenReturn(authentication);
        when(authorizationService.isAdmin(authentication)).thenReturn(false);
        when(authorizationService.getAuthenticatedUser()).thenReturn(testUser);
        when(authorizationService.isDistributorMember(10L, testUser)).thenReturn(true);
        when(distributorRepository.findById(10L)).thenReturn(Optional.of(distributor));

        CylinderInventory inventory = new CylinderInventory(distributor, "DOMESTIC_14_2", 14.2, 80, 15, 0, 1, "test");
        when(inventoryRepository.findByDistributorIdAndCylinderCategory(10L, "DOMESTIC_14_2"))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(CylinderInventory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.findByDistributorIdAndStatus(10L, "CONFIRMED")).thenReturn(List.of());

        CylinderInventoryUpdateDTO updateDTO = new CylinderInventoryUpdateDTO();
        updateDTO.setCylinderCategory("DOMESTIC_14_2");
        updateDTO.setFilledStock(120);
        updateDTO.setEmptyStock(25);
        updateDTO.setDamagedStock(2);
        updateDTO.setReason("Depot tanker refill received");
        updateDTO.setReferenceNumber("REFILL-8831");

        CylinderInventoryResponseDTO response = inventoryService.updateStock(10L, updateDTO);

        assertThat(response.getFilledStock()).isEqualTo(120);
        assertThat(response.getEmptyStock()).isEqualTo(25);
        assertThat(response.getDamagedStock()).isEqualTo(2);
        assertThat(response.getAvailableStock()).isEqualTo(120);
        assertThat(response.getLastAuditedBy()).isEqualTo("operator@apexgas.com");

        verify(movementRepository).save(any(InventoryMovement.class));
    }

    @Test
    @DisplayName("getInventoryMovements: returns auditable movement history for a category")
    void getInventoryMovements_success() {
        when(authorizationService.getAuthentication()).thenReturn(authentication);
        when(authorizationService.isAdmin(authentication)).thenReturn(false);
        when(authorizationService.getAuthenticatedUser()).thenReturn(testUser);
        when(authorizationService.isDistributorMember(10L, testUser)).thenReturn(true);

        CylinderInventory inventory = new CylinderInventory(distributor, "DOMESTIC_14_2", 14.2, 120, 25, 0, 2, "test");
        inventory.setId(50L);
        when(inventoryRepository.findByDistributorIdAndCylinderCategory(10L, "DOMESTIC_14_2"))
                .thenReturn(Optional.of(inventory));

        InventoryMovement movement = new InventoryMovement(
                inventory, "STOCK_RECEIPT", 40, "Truck unloading", "REF-123", "operator@apexgas.com");
        when(movementRepository.findByInventoryIdOrderByPerformedAtDesc(50L)).thenReturn(List.of(movement));

        List<InventoryMovementResponseDTO> movements = inventoryService.getInventoryMovements(10L, "DOMESTIC_14_2");

        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).getMovementType()).isEqualTo("STOCK_RECEIPT");
        assertThat(movements.get(0).getQuantityChange()).isEqualTo(40);
        assertThat(movements.get(0).getReferenceNumber()).isEqualTo("REF-123");
    }

    @Test
    @DisplayName("getDashboardOverview: aggregates real operational counts, queue age, inventory, and transparency")
    void getDashboardOverview_success() {
        when(authorizationService.getAuthentication()).thenReturn(authentication);
        when(authorizationService.isAdmin(authentication)).thenReturn(false);
        when(authorizationService.getAuthenticatedUser()).thenReturn(testUser);
        when(authorizationService.isDistributorMember(10L, testUser)).thenReturn(true);
        when(distributorRepository.findById(10L)).thenReturn(Optional.of(distributor));

        // Setup bookings: 1 PENDING, 1 CONFIRMED
        Booking bPending = new Booking();
        bPending.setId(201L);
        bPending.setStatus("PENDING");
        bPending.setBookingDate(LocalDateTime.now().minusHours(2));

        Booking bConfirmed = new Booking();
        bConfirmed.setId(202L);
        bConfirmed.setStatus("CONFIRMED");
        bConfirmed.setBookingDate(LocalDateTime.now().minusHours(24));
        bConfirmed.setCylinderCategory("DOMESTIC_14_2");
        bConfirmed.setQuantity(1);

        when(bookingRepository.findByDistributorIdWithDetails(10L)).thenReturn(List.of(bPending, bConfirmed));
        when(bookingRepository.findByDistributorIdAndStatus(10L, "CONFIRMED")).thenReturn(List.of(bConfirmed));

        // Setup deliveries: 1 ASSIGNED, 1 DELIVERED
        Delivery dAssigned = new Delivery();
        dAssigned.setId(301L);
        dAssigned.setStatus("ASSIGNED");

        Delivery dDelivered = new Delivery();
        dDelivered.setId(302L);
        dDelivered.setStatus("DELIVERED");

        when(deliveryRepository.findByDistributorIdWithDetails(10L)).thenReturn(List.of(dAssigned, dDelivered));

        // Setup inventory
        CylinderInventory inv = new CylinderInventory(distributor, "DOMESTIC_14_2", 14.2, 50, 10, 0, 0, "test");
        when(inventoryRepository.findByDistributorId(10L)).thenReturn(new ArrayList<>(List.of(inv)));
        when(inventoryRepository.save(any(CylinderInventory.class))).thenAnswer(i -> i.getArgument(0));

        // Setup transparency report
        TransparencyReportDTO transparencyReport = new TransparencyReportDTO();
        transparencyReport.setTotalAllocated(500);
        transparencyReport.setTotalDelivered(150);
        transparencyReport.setTotalRemaining(350);
        transparencyReport.setCategories(List.of());
        when(allocationService.getTransparencyReport(eq(10L), anyInt(), anyInt(), any()))
                .thenReturn(transparencyReport);

        DistributorDashboardOverviewDTO overview = inventoryService.getDashboardOverview(10L);

        assertThat(overview.getDistributorId()).isEqualTo(10L);
        assertThat(overview.getNewBookingRequestsCount()).isEqualTo(1);
        assertThat(overview.getAcceptedQueuedOrdersCount()).isEqualTo(1);
        assertThat(overview.getScheduledDeliveriesCount()).isEqualTo(1);
        assertThat(overview.getCompletedDeliveriesCount()).isEqualTo(1);
        assertThat(overview.getAverageQueueAgeHours()).isGreaterThan(20.0);
        assertThat(overview.getAllocatedMonthlyTotal()).isEqualTo(500);
        assertThat(overview.getSoldMonthlyTotal()).isEqualTo(150);
        assertThat(overview.getRemainingAllocationTotal()).isEqualTo(350);
    }
}
