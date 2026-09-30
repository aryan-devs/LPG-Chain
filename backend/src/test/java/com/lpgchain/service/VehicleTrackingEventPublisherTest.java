package com.lpgchain.service;

import com.lpgchain.dto.VehicleLocationEventDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.entity.VehicleLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VehicleTrackingEventPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private VehicleTrackingEventPublisher publisher;
    private Vehicle vehicle;
    private VehicleLocation location;

    @BeforeEach
    void setUp() {
        publisher = new VehicleTrackingEventPublisher(messagingTemplate);

        Distributor distributor = new Distributor();
        distributor.setId(10L);
        distributor.setAgencyName("Apex Gas");

        vehicle = new Vehicle("MH-12-AB-1234", "LPG_DELIVERY", 30, "AVAILABLE", distributor);
        vehicle.setId(50L);

        location = new VehicleLocation(
                vehicle,
                18.6298,
                73.7997,
                32.5,
                180.0,
                LocalDateTime.of(2026, 9, 19, 10, 30, 0)
        );
        location.setId(100L);
    }

    @Test
    @DisplayName("1. Valid event published to correct destination (/topic/deliveries/{id}/tracking)")
    void publishLocationUpdate_ValidEvent_PublishesToCorrectDestination() {
        // ARRANGE
        Long deliveryId = 25L;
        String expectedTopic = "/topic/deliveries/25/tracking";

        // ACT
        publisher.publishLocationUpdate(deliveryId, location);

        // ASSERT & VERIFY
        ArgumentCaptor<VehicleLocationEventDTO> captor = ArgumentCaptor.forClass(VehicleLocationEventDTO.class);
        verify(messagingTemplate).convertAndSend(eq(expectedTopic), captor.capture());

        VehicleLocationEventDTO event = captor.getValue();
        assertNotNull(event);
        assertEquals(25L, event.getDeliveryId());
        assertEquals(50L, event.getVehicleId());
        assertEquals("MH-12-AB-1234", event.getVehicleNumber());
        assertEquals(18.6298, event.getLatitude());
        assertEquals(73.7997, event.getLongitude());
        assertEquals(32.5, event.getSpeed());
        assertEquals(180.0, event.getHeading());
        assertEquals(LocalDateTime.of(2026, 9, 19, 10, 30, 0), event.getRecordedAt());
    }

    @Test
    @DisplayName("2. Correct delivery ID used across multiple deliveries")
    void publishLocationUpdate_DifferentDeliveryIds_UsesCorrectTopics() {
        // ARRANGE & ACT
        publisher.publishLocationUpdate(99L, location);

        // VERIFY
        verify(messagingTemplate).convertAndSend(eq("/topic/deliveries/99/tracking"), any(VehicleLocationEventDTO.class));
    }

    @Test
    @DisplayName("3. Correct event DTO generated with accurate coordinate and velocity mapping")
    void publishLocationUpdate_AccurateMapping() {
        // ARRANGE
        Long deliveryId = 15L;

        // ACT
        publisher.publishLocationUpdate(deliveryId, location);

        // ASSERT & VERIFY
        ArgumentCaptor<VehicleLocationEventDTO> captor = ArgumentCaptor.forClass(VehicleLocationEventDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/deliveries/15/tracking"), captor.capture());

        VehicleLocationEventDTO event = captor.getValue();
        assertEquals(18.6298, event.getLatitude());
        assertEquals(73.7997, event.getLongitude());
        assertEquals(32.5, event.getSpeed());
        assertEquals(180.0, event.getHeading());
    }

    @Test
    @DisplayName("4. No sensitive information included in the DTO or event serialization")
    void publishLocationUpdate_NoSensitiveInformationExposed() {
        // ARRANGE
        Long deliveryId = 25L;

        // ACT
        publisher.publishLocationUpdate(deliveryId, location);

        // ASSERT & VERIFY
        ArgumentCaptor<VehicleLocationEventDTO> captor = ArgumentCaptor.forClass(VehicleLocationEventDTO.class);
        verify(messagingTemplate).convertAndSend(any(String.class), captor.capture());

        String dtoString = captor.getValue().toString();
        assertFalse(dtoString.contains("password"), "Event must not contain password");
        assertFalse(dtoString.contains("jwt"), "Event must not contain JWT");
        assertFalse(dtoString.contains("traccar"), "Event must not contain Traccar credentials");
        assertFalse(dtoString.contains("token"), "Event must not contain token");
    }

    @Test
    @DisplayName("5. Null deliveryId or null location -> skips publishing safely")
    void publishLocationUpdate_NullArguments_SkipsPublishing() {
        // ACT
        publisher.publishLocationUpdate(null, location);
        publisher.publishLocationUpdate(25L, (VehicleLocation) null);
        publisher.publishLocationUpdate(null, (VehicleLocationEventDTO) null);

        // VERIFY
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("6. Pre-constructed event DTO published directly to expected topic")
    void publishLocationUpdate_DirectDTO_PublishesDirectly() {
        // ARRANGE
        VehicleLocationEventDTO event = new VehicleLocationEventDTO(
                40L, 50L, "MH-12-AB-1234", 18.5, 73.8, 45.0, 90.0, LocalDateTime.now()
        );

        // ACT
        publisher.publishLocationUpdate(40L, event);

        // VERIFY
        verify(messagingTemplate).convertAndSend(eq("/topic/deliveries/40/tracking"), eq(event));
    }
}
