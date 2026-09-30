package com.lpgchain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lpgchain.config.RedisProperties;
import com.lpgchain.dto.VehicleLocationEventDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleTrackingRedisPubSubTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private VehicleTrackingEventPublisher vehicleTrackingEventPublisher;

    private ObjectMapper objectMapper;
    private RedisProperties redisProperties;
    private VehicleTrackingRedisPublisher publisher;
    private VehicleTrackingRedisSubscriber subscriber;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        redisProperties = new RedisProperties();
        redisProperties.setTrackingChannel("lpgchain:tracking:vehicle-location");
        redisProperties.setLatestLocationTtlSeconds(60);

        publisher = new VehicleTrackingRedisPublisher(stringRedisTemplate, objectMapper, redisProperties);
        subscriber = new VehicleTrackingRedisSubscriber(objectMapper, vehicleTrackingEventPublisher);
    }

    @Test
    @DisplayName("1. publishTrackingEvent publishes valid JSON to configured Redis tracking channel")
    void publishTrackingEvent_ValidEvent_PublishesToCorrectChannel() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 9, 29, 22, 45, 0);
        VehicleLocationEventDTO event = new VehicleLocationEventDTO(
                501L, 101L, "MH-12-AB-1234", 18.6298, 73.7997, 35.0, 180.0, timestamp
        );

        boolean published = publisher.publishTrackingEvent(event);

        assertTrue(published);
        verify(stringRedisTemplate).convertAndSend(eq("lpgchain:tracking:vehicle-location"), anyString());
    }

    @Test
    @DisplayName("2. publishTrackingEvent JSON payload contains all mandatory telemetry fields")
    void publishTrackingEvent_PayloadContainsAllRequiredFields() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2026, 9, 29, 22, 45, 0);
        VehicleLocationEventDTO event = new VehicleLocationEventDTO(
                501L, 101L, "MH-12-AB-1234", 18.6298, 73.7997, 35.0, 180.0, timestamp
        );

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        boolean published = publisher.publishTrackingEvent(event);

        assertTrue(published);
        verify(stringRedisTemplate).convertAndSend(eq("lpgchain:tracking:vehicle-location"), payloadCaptor.capture());

        String json = payloadCaptor.getValue();
        assertNotNull(json);

        JsonNode root = objectMapper.readTree(json);
        assertEquals(501L, root.get("deliveryId").asLong());
        assertEquals(101L, root.get("vehicleId").asLong());
        assertEquals("MH-12-AB-1234", root.get("vehicleNumber").asText());
        assertEquals(18.6298, root.get("latitude").asDouble());
        assertEquals(73.7997, root.get("longitude").asDouble());
        assertEquals(35.0, root.get("speed").asDouble());
        assertEquals(180.0, root.get("heading").asDouble());
        assertNotNull(root.get("recordedAt"));
    }

    @Test
    @DisplayName("3. publishTrackingEvent with null event or null deliveryId returns false without publishing")
    void publishTrackingEvent_NullEventOrDeliveryId_ReturnsFalse() {
        assertFalse(publisher.publishTrackingEvent(null));
        assertFalse(publisher.publishTrackingEvent(new VehicleLocationEventDTO()));
        verify(stringRedisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("4. publishTrackingEvent handles Redis exceptions gracefully and returns false")
    void publishTrackingEvent_RedisException_ReturnsFalseGracefully() {
        when(stringRedisTemplate.convertAndSend(anyString(), anyString()))
                .thenThrow(new RedisSystemException("Redis server unavailable", new RuntimeException()));

        VehicleLocationEventDTO event = new VehicleLocationEventDTO(
                501L, 101L, "MH-12-AB-1234", 18.6298, 73.7997, 35.0, 180.0, LocalDateTime.now()
        );

        boolean result = publisher.publishTrackingEvent(event);
        assertFalse(result);
    }

    @Test
    @DisplayName("5. subscriber onMessage receives event and forwards to WebSocket publisher")
    void subscriber_onMessage_ValidMessage_ForwardsToWebSocketPublisher() throws Exception {
        LocalDateTime timestamp = LocalDateTime.of(2026, 9, 29, 22, 45, 0);
        VehicleLocationEventDTO event = new VehicleLocationEventDTO(
                501L, 101L, "MH-12-AB-1234", 18.6298, 73.7997, 35.0, 180.0, timestamp
        );

        byte[] payloadBytes = objectMapper.writeValueAsBytes(event);
        byte[] channelBytes = "lpgchain:tracking:vehicle-location".getBytes(StandardCharsets.UTF_8);
        Message redisMessage = new DefaultMessage(channelBytes, payloadBytes);

        subscriber.onMessage(redisMessage, null);

        ArgumentCaptor<VehicleLocationEventDTO> captor = ArgumentCaptor.forClass(VehicleLocationEventDTO.class);
        verify(vehicleTrackingEventPublisher).publishLocationUpdate(eq(501L), captor.capture());

        VehicleLocationEventDTO forwarded = captor.getValue();
        assertNotNull(forwarded);
        assertEquals(501L, forwarded.getDeliveryId());
        assertEquals(101L, forwarded.getVehicleId());
        assertEquals("MH-12-AB-1234", forwarded.getVehicleNumber());
        assertEquals(18.6298, forwarded.getLatitude());
        assertEquals(73.7997, forwarded.getLongitude());
    }

    @Test
    @DisplayName("6. subscriber onMessage handles corrupted JSON bytes safely without throwing")
    void subscriber_onMessage_CorruptedJson_HandlesSafely() {
        byte[] corrupted = "NOT_A_VALID_JSON{:::".getBytes(StandardCharsets.UTF_8);
        Message redisMessage = new DefaultMessage("channel".getBytes(StandardCharsets.UTF_8), corrupted);

        assertDoesNotThrow(() -> subscriber.onMessage(redisMessage, null));
        verify(vehicleTrackingEventPublisher, never()).publishLocationUpdate(any(), any(VehicleLocationEventDTO.class));
    }

    @Test
    @DisplayName("7. subscriber onMessage handles null or empty message safely")
    void subscriber_onMessage_NullOrEmpty_HandlesSafely() {
        assertDoesNotThrow(() -> subscriber.onMessage(null, null));

        Message emptyMessage = new DefaultMessage("channel".getBytes(StandardCharsets.UTF_8), new byte[0]);
        assertDoesNotThrow(() -> subscriber.onMessage(emptyMessage, null));

        verify(vehicleTrackingEventPublisher, never()).publishLocationUpdate(any(), any(VehicleLocationEventDTO.class));
    }

    @Test
    @DisplayName("8. subscriber onMessage with missing deliveryId discards message safely")
    void subscriber_onMessage_MissingDeliveryId_DiscardsSafely() throws Exception {
        VehicleLocationEventDTO event = new VehicleLocationEventDTO(
                null, 101L, "MH-12-AB-1234", 18.6298, 73.7997, 35.0, 180.0, LocalDateTime.now()
        );
        byte[] bytes = objectMapper.writeValueAsBytes(event);
        Message redisMessage = new DefaultMessage("channel".getBytes(StandardCharsets.UTF_8), bytes);

        subscriber.onMessage(redisMessage, null);

        verify(vehicleTrackingEventPublisher, never()).publishLocationUpdate(any(), any(VehicleLocationEventDTO.class));
    }
}
