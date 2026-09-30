package com.lpgchain.integration;

import com.lpgchain.dto.VehicleLocationEventDTO;
import com.lpgchain.dto.VehicleLocationRequestDTO;
import com.lpgchain.dto.VehicleLocationResponseDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.Distributor;
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
import com.lpgchain.security.JwtTokenProvider;
import com.lpgchain.service.VehicleTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class VehicleTrackingEventFlowIntegrationTest {

    @Autowired
    private VehicleTrackingService vehicleTrackingService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

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

    private Vehicle vehicle;
    private Delivery activeDelivery;
    private User adminUser;

    @BeforeEach
    void setUp() {
        Distributor distributor = new Distributor();
        distributor.setId(10L);
        distributor.setAgencyName("Apex Gas Agency");

        vehicle = new Vehicle("MH-12-AB-1234", "LPG_DELIVERY", 30, "AVAILABLE", distributor);
        vehicle.setId(101L);

        User customer = new User(1L, "Customer John", "customer@example.com", "hash");
        Booking booking = new Booking();
        booking.setId(201L);
        booking.setCustomer(customer);

        activeDelivery = new Delivery(booking, null, vehicle, distributor, "OUT_FOR_DELIVERY");
        activeDelivery.setId(501L);

        adminUser = new User(99L, "System Admin", "admin@lpgchain.example", "hash");
        Role adminRole = new Role(1L, "ADMIN");

        when(userRepository.findByEmail("admin@lpgchain.example")).thenReturn(Optional.of(adminUser));
        when(userRoleRepository.findByUser(adminUser)).thenReturn(List.of(new UserRole(1L, adminUser, adminRole)));

        // Set security context as ADMIN to authorize location ingestion
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin@lpgchain.example", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("End-to-end event flow: Record location -> Persist VehicleLocation -> Find active Delivery -> Publish STOMP event")
    void recordLocation_PersistsAndPublishesStompEventToCorrectDestination() {
        // ARRANGE
        LocalDateTime timestamp = LocalDateTime.of(2026, 9, 19, 14, 45, 0);
        VehicleLocationRequestDTO requestDTO = new VehicleLocationRequestDTO(
                18.6298, 73.7997, 35.0, 180.0, timestamp
        );

        VehicleLocation savedEntity = new VehicleLocation(
                vehicle, 18.6298, 73.7997, 35.0, 180.0, timestamp
        );
        savedEntity.setId(888L);

        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(vehicle));
        when(vehicleLocationRepository.save(any(VehicleLocation.class))).thenReturn(savedEntity);
        when(deliveryRepository.findFirstByVehicleIdAndStatusInOrderByAssignedAtDesc(eq(101L), any()))
                .thenReturn(Optional.of(activeDelivery));

        // ACT
        VehicleLocationResponseDTO response = vehicleTrackingService.recordLocation(101L, requestDTO);

        // ASSERT Service Response
        assertNotNull(response);
        assertEquals(888L, response.getId());
        assertEquals(101L, response.getVehicleId());
        assertEquals(18.6298, response.getLatitude());
        assertEquals(73.7997, response.getLongitude());

        // VERIFY Repository persistence
        verify(vehicleLocationRepository).save(any(VehicleLocation.class));

        // VERIFY STOMP Message broadcast to the exact delivery topic
        ArgumentCaptor<VehicleLocationEventDTO> captor = ArgumentCaptor.forClass(VehicleLocationEventDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/deliveries/501/tracking"), captor.capture());

        VehicleLocationEventDTO broadcastEvent = captor.getValue();
        assertNotNull(broadcastEvent);
        assertEquals(501L, broadcastEvent.getDeliveryId());
        assertEquals(101L, broadcastEvent.getVehicleId());
        assertEquals("MH-12-AB-1234", broadcastEvent.getVehicleNumber());
        assertEquals(18.6298, broadcastEvent.getLatitude());
        assertEquals(73.7997, broadcastEvent.getLongitude());
        assertEquals(35.0, broadcastEvent.getSpeed());
        assertEquals(180.0, broadcastEvent.getHeading());
        assertEquals(timestamp, broadcastEvent.getRecordedAt());
    }
}
