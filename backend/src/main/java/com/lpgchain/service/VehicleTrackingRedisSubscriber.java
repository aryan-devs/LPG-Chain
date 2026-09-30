package com.lpgchain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.dto.VehicleLocationEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import com.lpgchain.config.RedisProperties;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * VehicleTrackingRedisSubscriber — Listens to Redis Pub/Sub tracking channels,
 * deserializes incoming JSON payloads safely, and forwards them to the existing
 * WebSocket/STOMP broadcasting layer (VehicleTrackingEventPublisher).
 */
@Component
public class VehicleTrackingRedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(VehicleTrackingRedisSubscriber.class);

    private final ObjectMapper objectMapper;
    private final VehicleTrackingEventPublisher vehicleTrackingEventPublisher;
    private final RedisProperties redisProperties;

    @Autowired
    public VehicleTrackingRedisSubscriber(ObjectMapper objectMapper,
                                          VehicleTrackingEventPublisher vehicleTrackingEventPublisher,
                                          RedisProperties redisProperties) {
        this.objectMapper = objectMapper;
        this.vehicleTrackingEventPublisher = vehicleTrackingEventPublisher;
        this.redisProperties = redisProperties != null ? redisProperties : new RedisProperties();
    }

    public VehicleTrackingRedisSubscriber(ObjectMapper objectMapper,
                                          VehicleTrackingEventPublisher vehicleTrackingEventPublisher) {
        this(objectMapper, vehicleTrackingEventPublisher, new RedisProperties());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null || message.getBody() == null || message.getBody().length == 0) {
            log.warn("Received null or empty Redis message body");
            return;
        }

        try {
            String jsonPayload = new String(message.getBody(), StandardCharsets.UTF_8);
            VehicleLocationEventDTO event = objectMapper.readValue(jsonPayload, VehicleLocationEventDTO.class);

            if (event.getDeliveryId() == null) {
                log.warn("Discarding malformed Redis tracking event with missing deliveryId");
                return;
            }

            String instanceId = redisProperties.getInstanceId();
            log.info("Instance [{}] received Redis tracking event for delivery #{} (vehicle={})",
                    instanceId, event.getDeliveryId(), event.getVehicleNumber());
            log.debug("Instance [{}] forwarding tracking event for delivery #{} to STOMP WebSocket topic",
                    instanceId, event.getDeliveryId());
            vehicleTrackingEventPublisher.publishLocationUpdate(event.getDeliveryId(), event);
        } catch (Exception e) {
            log.error("Failed to deserialize or process Redis tracking message safely: {}", e.getMessage(), e);
        }
    }
}
