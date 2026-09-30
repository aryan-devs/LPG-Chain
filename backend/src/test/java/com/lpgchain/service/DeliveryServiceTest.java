package com.lpgchain.service;

import com.lpgchain.dto.DeliveryRequestDTO;
import com.lpgchain.dto.DeliveryResponseDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.Driver;
import com.lpgchain.entity.User;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.DriverRepository;
import com.lpgchain.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DistributorRepository distributorRepository;

    @Mock
    private DeliveryAuthorizationService deliveryAuthorizationService;

    private final EntityMapper entityMapper = new EntityMapper();

    private DeliveryService deliveryService;

    private Distributor distributor1;
    private Distributor distributor2;
    private User customer;
    private Booking booking;
    private Driver driver1;
    private Driver driver2;
    private Vehicle vehicle1;
    private Vehicle vehicle2;
    private Delivery delivery;

    @BeforeEach
    void setUp() {
        distributor1 = new Distributor();
        distributor1.setId(1L);
        distributor1.setAgencyName("Apex Gas");

        distributor2 = new Distributor();
        distributor2.setId(2L);
        distributor2.setAgencyName("Beta Gas");

        customer = new User(101L, "Rahul Customer", "customer@lpgchain.demo", "hash");

        booking = new Booking(1001L, customer, distributor1, "42 Marine Drive", LocalDateTime.now(), 2, "CONFIRMED");

        driver1 = new Driver(10L, "Rajesh Driver", "+919876543210", "LIC-111", "AVAILABLE", distributor1);
        driver2 = new Driver(20L, "Sunil Driver", "+919876543211", "LIC-222", "AVAILABLE", distributor2);

        vehicle1 = new Vehicle(5L, "MH-01-AA-1111", "LPG_DELIVERY", 30, "AVAILABLE", distributor1);
        vehicle2 = new Vehicle(6L, "MH-02-BB-2222", "LPG_DELIVERY", 25, "AVAILABLE", distributor2);

        delivery = new Delivery(501L, booking, driver1, vehicle1, distributor1, "ASSIGNED",
                LocalDateTime.now(), null, null, null);

        deliveryService = new DeliveryService(deliveryRepository, bookingRepository, driverRepository,
                vehicleRepository, distributorRepository, entityMapper, deliveryAuthorizationService);
    }

    @Test
    @DisplayName("createDelivery: succeeds when all 10 rules are satisfied")
    void createDelivery_Success() {
        // ARRANGE
        DeliveryRequestDTO dto = new DeliveryRequestDTO(1001L, 10L, 5L, 1L);
        when(bookingRepository.findById(1001L)).thenReturn(Optional.of(booking));
        when(deliveryRepository.existsByBookingIdAndStatusIn(eq(1001L), any())).thenReturn(false);
        when(driverRepository.findById(10L)).thenReturn(Optional.of(driver1));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle1));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> {
            Delivery d = inv.getArgument(0);
            d.setId(501L);
            return d;
        });

        // ACT
        DeliveryResponseDTO response = deliveryService.createDelivery(dto);

        // ASSERT
        assertNotNull(response);
        assertEquals(501L, response.getId());
        assertEquals("ASSIGNED", response.getStatus());
        assertEquals(10L, response.getDriverId());
        assertEquals(5L, response.getVehicleId());
        assertEquals(1L, response.getDistributorId());
        assertEquals("ASSIGNED", driver1.getStatus());
        assertEquals("ASSIGNED", vehicle1.getStatus());

        // VERIFY
        verify(deliveryAuthorizationService).checkCanCreateDelivery(1L);
        verify(driverRepository).save(driver1);
        verify(vehicleRepository).save(vehicle1);
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    @DisplayName("createDelivery: throws when booking does not exist")
    void createDelivery_MissingBooking() {
        // ARRANGE
        DeliveryRequestDTO dto = new DeliveryRequestDTO(9999L, 10L, 5L, 1L);
        when(bookingRepository.findById(9999L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.createDelivery(dto));
        assertTrue(ex.getMessage().contains("Booking not found"));
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDelivery: throws when booking is CANCELLED")
    void createDelivery_CancelledBooking() {
        // ARRANGE
        booking.setStatus("CANCELLED");
        DeliveryRequestDTO dto = new DeliveryRequestDTO(1001L, 10L, 5L, 1L);
        when(bookingRepository.findById(1001L)).thenReturn(Optional.of(booking));

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.createDelivery(dto));
        assertTrue(ex.getMessage().contains("CANCELLED"));
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDelivery: throws when booking has no distributor")
    void createDelivery_BookingMissingDistributor() {
        // ARRANGE
        booking.setDistributor(null);
        DeliveryRequestDTO dto = new DeliveryRequestDTO(1001L, 10L, 5L, 1L);
        when(bookingRepository.findById(1001L)).thenReturn(Optional.of(booking));

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.createDelivery(dto));
        assertTrue(ex.getMessage().contains("does not have an assigned distributor"));
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDelivery: throws when booking already has an active delivery")
    void createDelivery_DuplicateActiveDelivery() {
        // ARRANGE
        DeliveryRequestDTO dto = new DeliveryRequestDTO(1001L, 10L, 5L, 1L);
        when(bookingRepository.findById(1001L)).thenReturn(Optional.of(booking));
        when(deliveryRepository.existsByBookingIdAndStatusIn(eq(1001L), any())).thenReturn(true);

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.createDelivery(dto));
        assertTrue(ex.getMessage().contains("already has an active delivery"));
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDelivery: throws when driver does not exist")
    void createDelivery_MissingDriver() {
        // ARRANGE
        DeliveryRequestDTO dto = new DeliveryRequestDTO(1001L, 999L, 5L, 1L);
        when(bookingRepository.findById(1001L)).thenReturn(Optional.of(booking));
        when(deliveryRepository.existsByBookingIdAndStatusIn(eq(1001L), any())).thenReturn(false);
        when(driverRepository.findById(999L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.createDelivery(dto));
        assertTrue(ex.getMessage().contains("Driver not found"));
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDelivery: throws when driver belongs to another distributor")
    void createDelivery_DriverFromAnotherDistributor() {
        // ARRANGE
        DeliveryRequestDTO dto = new DeliveryRequestDTO(1001L, 20L, 5L, 1L);
        when(bookingRepository.findById(1001L)).thenReturn(Optional.of(booking));
        when(deliveryRepository.existsByBookingIdAndStatusIn(eq(1001L), any())).thenReturn(false);
        when(driverRepository.findById(20L)).thenReturn(Optional.of(driver2)); // driver2 belongs to distributor2
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle1));

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.createDelivery(dto));
        assertTrue(ex.getMessage().contains("does not belong to distributor"));
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDelivery: throws when vehicle belongs to another distributor")
    void createDelivery_VehicleFromAnotherDistributor() {
        // ARRANGE
        DeliveryRequestDTO dto = new DeliveryRequestDTO(1001L, 10L, 6L, 1L);
        when(bookingRepository.findById(1001L)).thenReturn(Optional.of(booking));
        when(deliveryRepository.existsByBookingIdAndStatusIn(eq(1001L), any())).thenReturn(false);
        when(driverRepository.findById(10L)).thenReturn(Optional.of(driver1));
        when(vehicleRepository.findById(6L)).thenReturn(Optional.of(vehicle2)); // vehicle2 belongs to distributor2

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.createDelivery(dto));
        assertTrue(ex.getMessage().contains("does not belong to distributor"));
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDelivery: throws when driver is not AVAILABLE")
    void createDelivery_DriverUnavailable() {
        // ARRANGE
        driver1.setStatus("ON_DELIVERY");
        DeliveryRequestDTO dto = new DeliveryRequestDTO(1001L, 10L, 5L, 1L);
        when(bookingRepository.findById(1001L)).thenReturn(Optional.of(booking));
        when(deliveryRepository.existsByBookingIdAndStatusIn(eq(1001L), any())).thenReturn(false);
        when(driverRepository.findById(10L)).thenReturn(Optional.of(driver1));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle1));

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.createDelivery(dto));
        assertTrue(ex.getMessage().contains("not AVAILABLE"));
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDelivery: throws when vehicle is not AVAILABLE")
    void createDelivery_VehicleUnavailable() {
        // ARRANGE
        vehicle1.setStatus("IN_TRANSIT");
        DeliveryRequestDTO dto = new DeliveryRequestDTO(1001L, 10L, 5L, 1L);
        when(bookingRepository.findById(1001L)).thenReturn(Optional.of(booking));
        when(deliveryRepository.existsByBookingIdAndStatusIn(eq(1001L), any())).thenReturn(false);
        when(driverRepository.findById(10L)).thenReturn(Optional.of(driver1));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle1));

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.createDelivery(dto));
        assertTrue(ex.getMessage().contains("not AVAILABLE"));
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateDeliveryStatus: completes full lifecycle and sets terminal DELIVERED")
    void updateDeliveryStatus_FullLifecycle() {
        // ARRANGE
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. ASSIGNED -> PICKED_UP
        DeliveryResponseDTO res1 = deliveryService.updateDeliveryStatus(501L, "PICKED_UP");
        assertEquals("PICKED_UP", res1.getStatus());
        assertNotNull(res1.getPickedUpAt());

        // 2. PICKED_UP -> OUT_FOR_DELIVERY
        DeliveryResponseDTO res2 = deliveryService.updateDeliveryStatus(501L, "OUT_FOR_DELIVERY");
        assertEquals("OUT_FOR_DELIVERY", res2.getStatus());
        assertNotNull(res2.getOutForDeliveryAt());
        assertEquals("ON_DELIVERY", driver1.getStatus());
        assertEquals("IN_TRANSIT", vehicle1.getStatus());

        // 3. OUT_FOR_DELIVERY -> DELIVERED
        DeliveryResponseDTO res3 = deliveryService.updateDeliveryStatus(501L, "DELIVERED");
        assertEquals("DELIVERED", res3.getStatus());
        assertNotNull(res3.getDeliveredAt());
        assertEquals("AVAILABLE", driver1.getStatus());
        assertEquals("AVAILABLE", vehicle1.getStatus());
        assertEquals("DELIVERED", booking.getStatus());
    }

    @Test
    @DisplayName("updateDeliveryStatus: rejects invalid transition ASSIGNED -> DELIVERED")
    void updateDeliveryStatus_InvalidTransition() {
        // ARRANGE
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(delivery));

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.updateDeliveryStatus(501L, "DELIVERED"));
        assertTrue(ex.getMessage().contains("Transition from ASSIGNED to DELIVERED is not allowed"));
    }

    @Test
    @DisplayName("updateDeliveryStatus: rejects any transition once DELIVERED (terminal state)")
    void updateDeliveryStatus_DeliveredIsTerminal() {
        // ARRANGE
        delivery.setStatus("DELIVERED");
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(delivery));

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.updateDeliveryStatus(501L, "PICKED_UP"));
        assertTrue(ex.getMessage().contains("Transition from DELIVERED to PICKED_UP is not allowed"));
    }
}
