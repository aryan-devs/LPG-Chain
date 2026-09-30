package com.lpgchain.service;

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
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * VehicleTrackingHorizontalScalingTest — Unit test suite verifying multi-instance
 * Redis Pub/Sub horizontal scaling and WebSocket/STOMP fanout behavior.
 *
 * Validates:
 * A. Same Redis channel across multiple Spring Boot instances
 * B. Event fan-out: Single published event received by multiple instance subscribers
 * C. Payload integrity across serialization / deserialization boundaries
 * D. WebSocket forwarding to delivery-specific topics (/topic/deliveries/{id}/tracking)
 * E. Duplicate/loop prevention: Redis subscriber never republishes back to Redis
 * F. Instance identification and independent local WebSocket template isolation
 */
@ExtendWith(MockitoExtension.class)
class VehicleTrackingHorizontalScalingTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate1;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate2;

    private ObjectMapper objectMapper;

    // Instance 1 components
    private RedisProperties instance1Properties;
    private VehicleTrackingEventPublisher instance1WebSocketPublisher;
    private VehicleTrackingRedisSubscriber instance1Subscriber;

    // Instance 2 components
    private RedisProperties instance2Properties;
    private VehicleTrackingEventPublisher instance2WebSocketPublisher;
    private VehicleTrackingRedisSubscriber instance2Subscriber;

    // Shared Redis Publisher (as used by any node)
    private VehicleTrackingRedisPublisher redisPublisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Instance 1 setup (e.g. running on port 8080)
        instance1Properties = new RedisProperties();
        instance1Properties.setInstanceId("lpg-chain-1");
        instance1Properties.setTrackingChannel("lpgchain:tracking:vehicle-location");
        instance1Properties.setLatestLocationTtlSeconds(60);
        instance1WebSocketPublisher = new VehicleTrackingEventPublisher(simpMessagingTemplate1);
        instance1Subscriber = new VehicleTrackingRedisSubscriber(objectMapper, instance1WebSocketPublisher, instance1Properties);

        // Instance 2 setup (e.g. running on port 8081)
        instance2Properties = new RedisProperties();
        instance2Properties.setInstanceId("lpg-chain-2");
        instance2Properties.setTrackingChannel("lpgchain:tracking:vehicle-location");
        instance2Properties.setLatestLocationTtlSeconds(60);
        instance2WebSocketPublisher = new VehicleTrackingEventPublisher(simpMessagingTemplate2);
        instance2Subscriber = new VehicleTrackingRedisSubscriber(objectMapper, instance2WebSocketPublisher, instance2Properties);

        // Redis publisher configured with shared tracking channel
        redisPublisher = new VehicleTrackingRedisPublisher(stringRedisTemplate, objectMapper, instance1Properties);
    }

    @Test
    @DisplayName("A. Multiple Spring Boot instances share identical Redis Pub/Sub tracking channel configuration")
    void multiInstance_SameRedisChannelConfiguration() {
        assertEquals("lpgchain:tracking:vehicle-location", instance1Properties.getTrackingChannel());
        assertEquals("lpgchain:tracking:vehicle-location", instance2Properties.getTrackingChannel());
        assertEquals(instance1Properties.getTrackingChannel(), instance2Properties.getTrackingChannel());

        // Each instance retains its distinct instance identifier
        assertEquals("lpg-chain-1", instance1Properties.getInstanceId());
        assertEquals("lpg-chain-2", instance2Properties.getInstanceId());
    }

    @Test
    @DisplayName("B. Event Fan-Out: Single Redis Pub/Sub message reaches multiple Spring Boot instances")
    void eventFanOut_SingleMessageReachesAllInstanceSubscribers() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 9, 29, 23, 15, 0);
        VehicleLocationEventDTO event = new VehicleLocationEventDTO(
                1001L, 55L, "MH-12-XY-9999", 18.5204, 73.8567, 42.0, 135.0, now
        );

        byte[] payloadBytes = objectMapper.writeValueAsBytes(event);
        byte[] channelBytes = "lpgchain:tracking:vehicle-location".getBytes(StandardCharsets.UTF_8);
        Message redisMessage = new DefaultMessage(channelBytes, payloadBytes);

        // Simulate Redis broadcasting the message to both registered subscribers
        instance1Subscriber.onMessage(redisMessage, null);
        instance2Subscriber.onMessage(redisMessage, null);

        // Instance 1 local WebSocket template should broadcast to its locally connected clients
        verify(simpMessagingTemplate1).convertAndSend(eq("/topic/deliveries/1001/tracking"), any(VehicleLocationEventDTO.class));

        // Instance 2 local WebSocket template should also broadcast to its locally connected clients
        verify(simpMessagingTemplate2).convertAndSend(eq("/topic/deliveries/1001/tracking"), any(VehicleLocationEventDTO.class));
    }

    @Test
    @DisplayName("C. Event Payload Integrity: All 8 telemetry fields preserved through fan-out distribution")
    void eventPayloadIntegrity_PreservedThroughMultiInstanceFanOut() throws Exception {
        LocalDateTime recordedAt = LocalDateTime.of(2026, 9, 29, 23, 20, 15);
        VehicleLocationEventDTO originalEvent = new VehicleLocationEventDTO(
                777L, 88L, "MH-14-ZZ-5555", 18.6298, 73.7997, 38.5, 275.0, recordedAt
        );

        byte[] messageBytes = objectMapper.writeValueAsBytes(originalEvent);
        Message redisMessage = new DefaultMessage("lpgchain:tracking:vehicle-location".getBytes(StandardCharsets.UTF_8), messageBytes);

        // Instance 1 receives message
        instance1Subscriber.onMessage(redisMessage, null);

        ArgumentCaptor<VehicleLocationEventDTO> captor = ArgumentCaptor.forClass(VehicleLocationEventDTO.class);
        verify(simpMessagingTemplate1).convertAndSend(eq("/topic/deliveries/777/tracking"), captor.capture());

        VehicleLocationEventDTO receivedEvent = captor.getValue();
        assertNotNull(receivedEvent);
        assertEquals(777L, receivedEvent.getDeliveryId());
        assertEquals(88L, receivedEvent.getVehicleId());
        assertEquals("MH-14-ZZ-5555", receivedEvent.getVehicleNumber());
        assertEquals(18.6298, receivedEvent.getLatitude(), 0.0001);
        assertEquals(73.7997, receivedEvent.getLongitude(), 0.0001);
        assertEquals(38.5, receivedEvent.getSpeed(), 0.0001);
        assertEquals(275.0, receivedEvent.getHeading(), 0.0001);
        assertEquals(recordedAt, receivedEvent.getRecordedAt());
    }

    @Test
    @DisplayName("D. WebSocket Forwarding: Delivered to delivery-specific topic, never to global broadcast")
    void webSocketForwarding_TargetedDeliveryTopicOnly() throws Exception {
        VehicleLocationEventDTO event = new VehicleLocationEventDTO(
                2048L, 99L, "MH-01-AA-1111", 19.0760, 72.8777, 50.0, 90.0, LocalDateTime.now()
        );
        byte[] bytes = objectMapper.writeValueAsBytes(event);
        Message message = new DefaultMessage("channel".getBytes(StandardCharsets.UTF_8), bytes);

        instance1Subscriber.onMessage(message, null);

        // Destination must be delivery-scoped (/topic/deliveries/2048/tracking)
        verify(simpMessagingTemplate1).convertAndSend(eq("/topic/deliveries/2048/tracking"), any(VehicleLocationEventDTO.class));
        verify(simpMessagingTemplate1, never()).convertAndSend(eq("/topic/tracking"), any(VehicleLocationEventDTO.class));
        verify(simpMessagingTemplate1, never()).convertAndSend(eq("/topic/all"), any(VehicleLocationEventDTO.class));
    }

    @Test
    @DisplayName("E. Loop / Duplicate Protection: Redis message arrival does NOT republish back to Redis")
    void duplicateProtection_ReceivedRedisMessage_NeverRePublishedToRedis() throws Exception {
        VehicleLocationEventDTO event = new VehicleLocationEventDTO(
                3001L, 12L, "MH-20-BB-2222", 18.5204, 73.8567, 25.0, 180.0, LocalDateTime.now()
        );
        byte[] payload = objectMapper.writeValueAsBytes(event);
        Message message = new DefaultMessage("lpgchain:tracking:vehicle-location".getBytes(StandardCharsets.UTF_8), payload);

        // Instance 1 processes incoming Redis message
        instance1Subscriber.onMessage(message, null);

        // Verify stringRedisTemplate.convertAndSend was NEVER invoked by the subscriber flow
        verify(stringRedisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("F. Publisher converts and sends to configured shared channel")
    void redisPublisher_PublishesToConfiguredSharedChannel() {
        VehicleLocationEventDTO event = new VehicleLocationEventDTO(
                4001L, 15L, "MH-31-CC-3333", 18.5204, 73.8567, 30.0, 0.0, LocalDateTime.now()
        );

        boolean published = redisPublisher.publishTrackingEvent(event);

        assertTrue(published);
        verify(stringRedisTemplate).convertAndSend(eq("lpgchain:tracking:vehicle-location"), anyString());
    }
}
