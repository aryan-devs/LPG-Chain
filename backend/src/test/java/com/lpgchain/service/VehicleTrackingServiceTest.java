package com.lpgchain.service;

import com.lpgchain.dto.VehicleLocationRequestDTO;
import com.lpgchain.dto.VehicleLocationResponseDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.User;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.entity.VehicleLocation;
import com.lpgchain.exception.ResourceNotFoundException;
import com.lpgchain.integration.traccar.TraccarClient;
import com.lpgchain.integration.traccar.TraccarPositionDTO;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.VehicleLocationRepository;
import com.lpgchain.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleTrackingServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleLocationRepository vehicleLocationRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryAuthorizationService deliveryAuthorizationService;

    @Mock
    private TraccarClient traccarClient;

    @Mock
    private VehicleTrackingEventPublisher vehicleTrackingEventPublisher;

    private final EntityMapper entityMapper = new EntityMapper();
    private VehicleTrackingService trackingService;

    private Distributor distributor;
    private Vehicle vehicle;
    private Delivery delivery;

    @BeforeEach
    void setUp() {
        trackingService = new VehicleTrackingServiceImpl(
                vehicleRepository,
                vehicleLocationRepository,
                deliveryRepository,
                deliveryAuthorizationService,
                entityMapper,
                traccarClient,
                vehicleTrackingEventPublisher
        );

        distributor = new Distributor();
        distributor.setId(1L);
        distributor.setAgencyName("Apex Gas");

        vehicle = new Vehicle("MH-01-AB-1234", "LPG_DELIVERY", 30, "AVAILABLE", distributor);
        vehicle.setId(5L);

        User customer = new User();
        customer.setId(100L);

        Booking booking = new Booking();
        booking.setId(200L);
        booking.setCustomer(customer);

        delivery = new Delivery(booking, null, vehicle, distributor, "ASSIGNED");
        delivery.setId(300L);
    }

    @Test
    @DisplayName("1. recordLocation: valid location saved successfully")
    void recordLocation_Success() {
        // ARRANGE
        VehicleLocationRequestDTO dto = new VehicleLocationRequestDTO(
                18.6298, 73.7997, 32.5, 180.0, LocalDateTime.of(2026, 9, 17, 18, 30, 0)
        );

        VehicleLocation savedLocation = new VehicleLocation(
                vehicle, 18.6298, 73.7997, 32.5, 180.0, LocalDateTime.of(2026, 9, 17, 18, 30, 0)
        );
        savedLocation.setId(10L);

        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
        when(vehicleLocationRepository.save(any(VehicleLocation.class))).thenReturn(savedLocation);

        // ACT
        VehicleLocationResponseDTO response = trackingService.recordLocation(5L, dto);

        // ASSERT
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(5L, response.getVehicleId());
        assertEquals("MH-01-AB-1234", response.getVehicleNumber());
        assertEquals(18.6298, response.getLatitude());
        assertEquals(73.7997, response.getLongitude());
        assertEquals(32.5, response.getSpeed());
        assertEquals(180.0, response.getHeading());

        // VERIFY
        verify(deliveryAuthorizationService).checkCanIngestVehicleLocation(vehicle);
        verify(vehicleLocationRepository).save(any(VehicleLocation.class));
    }

    @Test
    @DisplayName("2. recordLocation: invalid latitude rejected (< -90 or > 90)")
    void recordLocation_InvalidLatitude_ThrowsException() {
        // ARRANGE
        VehicleLocationRequestDTO dto = new VehicleLocationRequestDTO(
                95.0, 73.7997, 25.0, 90.0, LocalDateTime.now()
        );

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> trackingService.recordLocation(5L, dto));
        assertTrue(ex.getMessage().contains("Latitude"));

        // VERIFY
        verify(vehicleLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. recordLocation: invalid longitude rejected (< -180 or > 180)")
    void recordLocation_InvalidLongitude_ThrowsException() {
        // ARRANGE
        VehicleLocationRequestDTO dto = new VehicleLocationRequestDTO(
                18.6298, 195.0, 25.0, 90.0, LocalDateTime.now()
        );

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> trackingService.recordLocation(5L, dto));
        assertTrue(ex.getMessage().contains("Longitude"));

        // VERIFY
        verify(vehicleLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("4. recordLocation: negative speed rejected")
    void recordLocation_NegativeSpeed_ThrowsException() {
        // ARRANGE
        VehicleLocationRequestDTO dto = new VehicleLocationRequestDTO(
                18.6298, 73.7997, -5.0, 90.0, LocalDateTime.now()
        );

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> trackingService.recordLocation(5L, dto));
        assertTrue(ex.getMessage().contains("Speed"));

        // VERIFY
        verify(vehicleLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. recordLocation: invalid heading rejected (>= 360 or < 0)")
    void recordLocation_InvalidHeading_ThrowsException() {
        // ARRANGE
        VehicleLocationRequestDTO dto = new VehicleLocationRequestDTO(
                18.6298, 73.7997, 20.0, 360.0, LocalDateTime.now()
        );

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> trackingService.recordLocation(5L, dto));
        assertTrue(ex.getMessage().contains("Heading"));

        // VERIFY
        verify(vehicleLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. recordLocation: missing vehicle rejected with 404")
    void recordLocation_MissingVehicle_ThrowsException() {
        // ARRANGE
        VehicleLocationRequestDTO dto = new VehicleLocationRequestDTO(
                18.6298, 73.7997, 20.0, 180.0, LocalDateTime.now()
        );
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(ResourceNotFoundException.class,
                () -> trackingService.recordLocation(99L, dto));

        // VERIFY
        verify(vehicleLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. getLatestLocation: returns latest recorded location")
    void getLatestLocation_Success() {
        // ARRANGE
        VehicleLocation loc = new VehicleLocation(
                vehicle, 18.6298, 73.7997, 30.0, 180.0, LocalDateTime.now()
        );
        loc.setId(1L);

        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
        when(vehicleLocationRepository.findFirstByVehicleIdOrderByRecordedAtDesc(5L)).thenReturn(Optional.of(loc));

        // ACT
        Optional<VehicleLocationResponseDTO> result = trackingService.getLatestLocation(5L);

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals(5L, result.get().getVehicleId());
        assertEquals(18.6298, result.get().getLatitude());

        // VERIFY
        verify(deliveryAuthorizationService).checkCanAccessVehicleTracking(vehicle);
    }

    @Test
    @DisplayName("8. getLocationHistory: retrieves location history with limit")
    void getLocationHistory_Success() {
        // ARRANGE
        VehicleLocation loc1 = new VehicleLocation(vehicle, 18.6298, 73.7997, 30.0, 180.0, LocalDateTime.now());
        loc1.setId(1L);
        VehicleLocation loc2 = new VehicleLocation(vehicle, 18.6200, 73.7900, 20.0, 170.0, LocalDateTime.now().minusMinutes(5));
        loc2.setId(2L);

        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
        when(vehicleLocationRepository.findByVehicleIdOrderByRecordedAtDesc(eq(5L), any(Pageable.class)))
                .thenReturn(List.of(loc1, loc2));

        // ACT
        List<VehicleLocationResponseDTO> history = trackingService.getLocationHistory(5L, 10);

        // ASSERT
        assertNotNull(history);
        assertEquals(2, history.size());
        assertEquals(1L, history.get(0).getId());

        // VERIFY
        verify(deliveryAuthorizationService).checkCanAccessVehicleTracking(vehicle);
    }

    @Test
    @DisplayName("9. setTraccarDeviceId: sets and saves Traccar device ID on vehicle")
    void setTraccarDeviceId_Success() {
        // ARRANGE
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));

        // ACT
        trackingService.setTraccarDeviceId(5L, "784521");

        // ASSERT
        assertEquals("784521", vehicle.getTraccarDeviceId());

        // VERIFY
        verify(deliveryAuthorizationService).checkCanManageDistributorResource(1L);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    @DisplayName("10. syncLocationFromTraccar: missing Traccar device mapping returns empty")
    void syncLocationFromTraccar_MissingDeviceId_ReturnsEmpty() {
        // ARRANGE
        vehicle.setTraccarDeviceId(null);
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));

        // ACT
        Optional<VehicleLocationResponseDTO> result = trackingService.syncLocationFromTraccar(5L);

        // ASSERT
        assertFalse(result.isPresent());

        // VERIFY
        verify(traccarClient, never()).getLatestPosition(any());
        verify(vehicleLocationRepository, never()).save(any());
    }

    @Test
    @DisplayName("11. getLatestLocationByDeliveryId: delivery location retrieval succeeds")
    void getLatestLocationByDeliveryId_Success() {
        // ARRANGE
        VehicleLocation loc = new VehicleLocation(vehicle, 18.6298, 73.7997, 30.0, 180.0, LocalDateTime.now());
        loc.setId(50L);

        when(deliveryRepository.findById(300L)).thenReturn(Optional.of(delivery));
        when(vehicleLocationRepository.findFirstByVehicleIdOrderByRecordedAtDesc(5L)).thenReturn(Optional.of(loc));

        // ACT
        Optional<VehicleLocationResponseDTO> result = trackingService.getLatestLocationByDeliveryId(300L);

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals(50L, result.get().getId());
        assertEquals(5L, result.get().getVehicleId());

        // VERIFY
        verify(deliveryAuthorizationService).checkCanAccessDelivery(delivery);
    }

    @Test
    @DisplayName("12. getLatestLocationByDeliveryId: no location available returns empty")
    void getLatestLocationByDeliveryId_NoLocation_ReturnsEmpty() {
        // ARRANGE
        when(deliveryRepository.findById(300L)).thenReturn(Optional.of(delivery));
        when(vehicleLocationRepository.findFirstByVehicleIdOrderByRecordedAtDesc(5L)).thenReturn(Optional.empty());

        // ACT
        Optional<VehicleLocationResponseDTO> result = trackingService.getLatestLocationByDeliveryId(300L);

        // ASSERT
        assertFalse(result.isPresent());

        // VERIFY
        verify(deliveryAuthorizationService).checkCanAccessDelivery(delivery);
    }

    @Test
    @DisplayName("13. recordLocation: active delivery found -> tracking event published")
    void recordLocation_WithActiveDelivery_PublishesTrackingEvent() {
        // ARRANGE
        VehicleLocationRequestDTO dto = new VehicleLocationRequestDTO(
                18.6298, 73.7997, 32.5, 180.0, LocalDateTime.of(2026, 9, 17, 18, 30, 0)
        );

        VehicleLocation savedLocation = new VehicleLocation(
                vehicle, 18.6298, 73.7997, 32.5, 180.0, LocalDateTime.of(2026, 9, 17, 18, 30, 0)
        );
        savedLocation.setId(10L);

        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
        when(vehicleLocationRepository.save(any(VehicleLocation.class))).thenReturn(savedLocation);
        when(deliveryRepository.findFirstByVehicleIdAndStatusInOrderByAssignedAtDesc(eq(5L), any()))
                .thenReturn(Optional.of(delivery));

        // ACT
        VehicleLocationResponseDTO response = trackingService.recordLocation(5L, dto);

        // ASSERT
        assertNotNull(response);

        // VERIFY
        verify(vehicleTrackingEventPublisher).publishLocationUpdate(300L, savedLocation);
    }

    @Test
    @DisplayName("14. recordLocation: no active delivery -> no tracking event published")
    void recordLocation_NoActiveDelivery_DoesNotPublishEvent() {
        // ARRANGE
        VehicleLocationRequestDTO dto = new VehicleLocationRequestDTO(
                18.6298, 73.7997, 32.5, 180.0, LocalDateTime.of(2026, 9, 17, 18, 30, 0)
        );

        VehicleLocation savedLocation = new VehicleLocation(
                vehicle, 18.6298, 73.7997, 32.5, 180.0, LocalDateTime.of(2026, 9, 17, 18, 30, 0)
        );
        savedLocation.setId(10L);

        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
        when(vehicleLocationRepository.save(any(VehicleLocation.class))).thenReturn(savedLocation);
        when(deliveryRepository.findFirstByVehicleIdAndStatusInOrderByAssignedAtDesc(eq(5L), any()))
                .thenReturn(Optional.empty());

        // ACT
        VehicleLocationResponseDTO response = trackingService.recordLocation(5L, dto);

        // ASSERT
        assertNotNull(response);

        // VERIFY
        verify(vehicleTrackingEventPublisher, never()).publishLocationUpdate(any(), any(VehicleLocation.class));
    }

    @Test
    @DisplayName("15. recordLocation: database save failure -> no event published")
    void recordLocation_DatabaseFailure_ThrowsExceptionAndNoEvent() {
        // ARRANGE
        VehicleLocationRequestDTO dto = new VehicleLocationRequestDTO(
                18.6298, 73.7997, 32.5, 180.0, LocalDateTime.of(2026, 9, 17, 18, 30, 0)
        );

        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
        when(vehicleLocationRepository.save(any(VehicleLocation.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("DB save failed"));

        // ACT & ASSERT
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> trackingService.recordLocation(5L, dto));

        // VERIFY
        verify(vehicleTrackingEventPublisher, never()).publishLocationUpdate(any(), any(VehicleLocation.class));
    }

    @Test
    @DisplayName("16. syncLocationFromTraccar: active delivery found -> publishes tracking event")
    void syncLocationFromTraccar_WithActiveDelivery_PublishesEvent() {
        // ARRANGE
        vehicle.setTraccarDeviceId("dev123");
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));

        TraccarPositionDTO pos = new TraccarPositionDTO();
        pos.setLatitude(18.6298);
        pos.setLongitude(73.7997);
        pos.setSpeed(25.0);
        pos.setCourse(180.0);
        pos.setFixTime("2026-09-17T18:30:00Z");

        when(traccarClient.getLatestPosition("dev123")).thenReturn(Optional.of(pos));

        VehicleLocation savedLocation = new VehicleLocation(
                vehicle, 18.6298, 73.7997, 25.0, 180.0, LocalDateTime.now()
        );
        savedLocation.setId(77L);
        when(vehicleLocationRepository.save(any(VehicleLocation.class))).thenReturn(savedLocation);
        when(deliveryRepository.findFirstByVehicleIdAndStatusInOrderByAssignedAtDesc(eq(5L), any()))
                .thenReturn(Optional.of(delivery));

        // ACT
        Optional<VehicleLocationResponseDTO> result = trackingService.syncLocationFromTraccar(5L);

        // ASSERT
        assertTrue(result.isPresent());

        // VERIFY
        verify(vehicleTrackingEventPublisher).publishLocationUpdate(300L, savedLocation);
    }
}
