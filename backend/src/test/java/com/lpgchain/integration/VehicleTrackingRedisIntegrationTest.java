package com.lpgchain.integration;

import com.lpgchain.dto.VehicleLocationEventDTO;
import com.lpgchain.dto.VehicleLocationRequestDTO;
import com.lpgchain.dto.VehicleLocationResponseDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.entity.VehicleLocation;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.repository.VehicleLocationRepository;
import com.lpgchain.repository.VehicleRepository;
import com.lpgchain.service.VehicleTrackingRedisPublisher;
import com.lpgchain.service.VehicleTrackingRedisService;
import com.lpgchain.service.VehicleTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class VehicleTrackingRedisIntegrationTest {

    @Autowired
    private VehicleTrackingService vehicleTrackingService;

    @MockBean
    private VehicleTrackingRedisService vehicleTrackingRedisService;

    @MockBean
    private VehicleTrackingRedisPublisher vehicleTrackingRedisPublisher;

    @MockBean
    private VehicleRepository vehicleRepository;

    @MockBean
    private VehicleLocationRepository vehicleLocationRepository;

    @MockBean
    private DeliveryRepository deliveryRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserRoleRepository userRoleRepository;

    @MockBean
    private DistributorRepository distributorRepository;

    @MockBean
    private DistributorMembershipRepository distributorMembershipRepository;

    private Distributor distributor1;
    private Distributor distributor2;
    private Vehicle vehicle1;
    private User adminUser;
    private User customerUser1;
    private User customerUser2;
    private User distributorUser1;
    private Delivery delivery1;

    @BeforeEach
    void setUp() {
        distributor1 = new Distributor();
        distributor1.setId(10L);
        distributor1.setAgencyName("Agency Alpha");

        distributor2 = new Distributor();
        distributor2.setId(20L);
        distributor2.setAgencyName("Agency Beta");

        vehicle1 = new Vehicle("MH-12-AB-1234", "LPG_DELIVERY", 30, "AVAILABLE", distributor1);
        vehicle1.setId(101L);

        adminUser = new User(1L, "Admin User", "admin@lpgchain.example", "hash");
        customerUser1 = new User(2L, "Customer 1", "customer1@gmail.com", "hash");
        customerUser2 = new User(3L, "Customer 2", "customer2@gmail.com", "hash");
        distributorUser1 = new User(4L, "Distributor 1", "dist1@approved-distributor.example", "hash");

        Role adminRole = new Role(1L, "ADMIN");
        Role customerRole = new Role(2L, "CUSTOMER");
        Role distributorRole = new Role(3L, "DISTRIBUTOR");

        when(userRepository.findByEmail("admin@lpgchain.example")).thenReturn(Optional.of(adminUser));
        when(userRepository.findByEmail("customer1@gmail.com")).thenReturn(Optional.of(customerUser1));
        when(userRepository.findByEmail("customer2@gmail.com")).thenReturn(Optional.of(customerUser2));
        when(userRepository.findByEmail("dist1@approved-distributor.example")).thenReturn(Optional.of(distributorUser1));

        when(userRoleRepository.findByUser(adminUser)).thenReturn(List.of(new UserRole(1L, adminUser, adminRole)));
        when(userRoleRepository.findByUser(customerUser1)).thenReturn(List.of(new UserRole(2L, customerUser1, customerRole)));
        when(userRoleRepository.findByUser(customerUser2)).thenReturn(List.of(new UserRole(3L, customerUser2, customerRole)));
        when(userRoleRepository.findByUser(distributorUser1)).thenReturn(List.of(new UserRole(4L, distributorUser1, distributorRole)));

        DistributorMembership membership = new DistributorMembership(distributorUser1, distributor1, "OPERATOR", "ACTIVE");
        when(distributorMembershipRepository.findByUserId(4L)).thenReturn(List.of(membership));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(4L, 10L))
                .thenReturn(Optional.of(membership));

        Booking booking1 = new Booking();
        booking1.setId(201L);
        booking1.setCustomer(customerUser1);
        booking1.setDistributor(distributor1);

        delivery1 = new Delivery(booking1, null, vehicle1, distributor1, "OUT_FOR_DELIVERY");
        delivery1.setId(501L);

        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(vehicle1));
        when(deliveryRepository.findById(501L)).thenReturn(Optional.of(delivery1));
    }

    private void authenticateAs(String email, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("1. getLatestLocation on Redis hit returns directly without querying database")
    void getLatestLocation_RedisCacheHit_ReturnsFromRedisWithoutDbQuery() {
        authenticateAs("admin@lpgchain.example", "ADMIN");

        LocalDateTime time = LocalDateTime.of(2026, 9, 29, 22, 50, 0);
        VehicleLocationResponseDTO cachedDto = new VehicleLocationResponseDTO(
                999L, 101L, "MH-12-AB-1234", 18.6298, 73.7997, 40.0, 90.0, time
        );

        when(vehicleTrackingRedisService.getLatestLocation(101L)).thenReturn(Optional.of(cachedDto));

        Optional<VehicleLocationResponseDTO> result = vehicleTrackingService.getLatestLocation(101L);

        assertTrue(result.isPresent());
        assertEquals(999L, result.get().getId());
        assertEquals(18.6298, result.get().getLatitude());

        // Verify DB repository was NOT queried
        verify(vehicleLocationRepository, never()).findFirstByVehicleIdOrderByRecordedAtDesc(any());
    }

    @Test
    @DisplayName("2. getLatestLocation on Redis miss queries MySQL and backfills Redis cache")
    void getLatestLocation_RedisCacheMiss_QueriesDbAndBackfillsCache() {
        authenticateAs("admin@lpgchain.example", "ADMIN");

        when(vehicleTrackingRedisService.getLatestLocation(101L)).thenReturn(Optional.empty());

        LocalDateTime time = LocalDateTime.of(2026, 9, 29, 22, 50, 0);
        VehicleLocation dbEntity = new VehicleLocation(vehicle1, 18.6298, 73.7997, 40.0, 90.0, time);
        dbEntity.setId(888L);

        when(vehicleLocationRepository.findFirstByVehicleIdOrderByRecordedAtDesc(101L))
                .thenReturn(Optional.of(dbEntity));

        Optional<VehicleLocationResponseDTO> result = vehicleTrackingService.getLatestLocation(101L);

        assertTrue(result.isPresent());
        assertEquals(888L, result.get().getId());

        // Verify DB repository was queried and Redis was backfilled
        verify(vehicleLocationRepository).findFirstByVehicleIdOrderByRecordedAtDesc(101L);
        verify(vehicleTrackingRedisService).saveLatestLocation(any(VehicleLocationResponseDTO.class));
    }

    @Test
    @DisplayName("3. getLatestLocationByDeliveryId on Redis hit returns directly without querying database")
    void getLatestLocationByDeliveryId_RedisHit_ReturnsFromRedis() {
        authenticateAs("customer1@gmail.com", "CUSTOMER");

        LocalDateTime time = LocalDateTime.of(2026, 9, 29, 22, 50, 0);
        VehicleLocationResponseDTO cachedDto = new VehicleLocationResponseDTO(
                777L, 101L, "MH-12-AB-1234", 18.6298, 73.7997, 40.0, 90.0, time
        );

        when(vehicleTrackingRedisService.getLatestLocation(101L)).thenReturn(Optional.of(cachedDto));

        Optional<VehicleLocationResponseDTO> result = vehicleTrackingService.getLatestLocationByDeliveryId(501L);

        assertTrue(result.isPresent());
        assertEquals(777L, result.get().getId());
        verify(vehicleLocationRepository, never()).findFirstByVehicleIdOrderByRecordedAtDesc(any());
    }

    @Test
    @DisplayName("4. recordLocation persists to DB, saves to Redis latest location, and publishes Redis event")
    void recordLocation_PersistsToDbAndUpdatesRedisCacheAndPublishesEvent() {
        authenticateAs("admin@lpgchain.example", "ADMIN");

        LocalDateTime time = LocalDateTime.of(2026, 9, 29, 22, 50, 0);
        VehicleLocationRequestDTO requestDTO = new VehicleLocationRequestDTO(
                18.6298, 73.7997, 35.0, 180.0, time
        );

        VehicleLocation savedEntity = new VehicleLocation(vehicle1, 18.6298, 73.7997, 35.0, 180.0, time);
        savedEntity.setId(666L);

        when(vehicleLocationRepository.save(any(VehicleLocation.class))).thenReturn(savedEntity);
        when(deliveryRepository.findFirstByVehicleIdAndStatusInOrderByAssignedAtDesc(eq(101L), any()))
                .thenReturn(Optional.of(delivery1));
        when(vehicleTrackingRedisPublisher.publishTrackingEvent(any(VehicleLocationEventDTO.class))).thenReturn(true);

        VehicleLocationResponseDTO response = vehicleTrackingService.recordLocation(101L, requestDTO);

        assertNotNull(response);
        assertEquals(666L, response.getId());

        // Verify MySQL persistence
        verify(vehicleLocationRepository).save(any(VehicleLocation.class));

        // Verify Redis cache and Pub/Sub integration
        verify(vehicleTrackingRedisService).saveLatestLocation(eq(vehicle1), eq(savedEntity));
        verify(vehicleTrackingRedisPublisher).publishTrackingEvent(any(VehicleLocationEventDTO.class));
    }

    @Test
    @DisplayName("5. Customer cannot access another customer's delivery tracking (security check precedes cache)")
    void customer_CannotAccessOtherCustomerDeliveryTracking() {
        // Customer 2 attempts to view Customer 1's delivery tracking
        authenticateAs("customer2@gmail.com", "CUSTOMER");

        assertThrows(AccessDeniedException.class, () ->
                vehicleTrackingService.getLatestLocationByDeliveryId(501L));

        // Redis cache should never even be queried for unauthorized requests
        verify(vehicleTrackingRedisService, never()).getLatestLocation(any());
    }

    @Test
    @DisplayName("6. Distributor cannot access another distributor's vehicle tracking")
    void distributor_CannotAccessOtherDistributorVehicleTracking() {
        // Vehicle belongs to distributor 1 (ID 10)
        // User is member of distributor 2 (ID 20)
        User distUser2 = new User(5L, "Distributor 2", "dist2@approved-distributor.example", "hash");
        Role distributorRole = new Role(3L, "DISTRIBUTOR");
        when(userRepository.findByEmail("dist2@approved-distributor.example")).thenReturn(Optional.of(distUser2));
        when(userRoleRepository.findByUser(distUser2)).thenReturn(List.of(new UserRole(5L, distUser2, distributorRole)));

        DistributorMembership membership2 = new DistributorMembership(distUser2, distributor2, "OPERATOR", "ACTIVE");
        when(distributorMembershipRepository.findByUserId(5L)).thenReturn(List.of(membership2));

        authenticateAs("dist2@approved-distributor.example", "DISTRIBUTOR");

        assertThrows(AccessDeniedException.class, () ->
                vehicleTrackingService.getLatestLocation(101L));

        verify(vehicleTrackingRedisService, never()).getLatestLocation(any());
    }
}
