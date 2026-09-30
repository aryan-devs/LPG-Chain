package com.lpgchain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.config.RedisProperties;
import com.lpgchain.dto.VehicleLocationEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * VehicleTrackingRedisPublisher — Publishes real-time vehicle GPS tracking events
 * to Redis Pub/Sub channels for cross-instance fanout across multiple Spring Boot nodes.
 */
@Service
public class VehicleTrackingRedisPublisher {

    private static final Logger log = LoggerFactory.getLogger(VehicleTrackingRedisPublisher.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisProperties redisProperties;

    public VehicleTrackingRedisPublisher(StringRedisTemplate stringRedisTemplate,
                                         ObjectMapper objectMapper,
                                         RedisProperties redisProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.redisProperties = redisProperties;
    }

    /**
     * Publishes a vehicle tracking event DTO to the configured Redis Pub/Sub topic channel.
     *
     * @param event the event payload containing delivery and telemetry information
     * @return true if successfully published to Redis, false otherwise
     */
    public boolean publishTrackingEvent(VehicleLocationEventDTO event) {
        if (event == null || event.getDeliveryId() == null) {
            log.warn("Skipping Redis Pub/Sub publish: event or deliveryId is null");
            return false;
        }

        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            String channel = redisProperties.getTrackingChannel();
            log.debug("Publishing tracking event to Redis channel [{}]: deliveryId={}, vehicleId={}",
                    channel, event.getDeliveryId(), event.getVehicleId());
            stringRedisTemplate.convertAndSend(channel, jsonPayload);
            return true;
        } catch (Exception e) {
            log.warn("Failed to publish tracking event to Redis channel [{}] (graceful fallback): {}",
                    redisProperties.getTrackingChannel(), e.getMessage());
            return false;
        }
    }
}
