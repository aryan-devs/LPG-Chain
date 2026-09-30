package com.lpgchain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.config.RedisProperties;
import com.lpgchain.dto.VehicleLocationResponseDTO;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.entity.VehicleLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;

/**
 * VehicleTrackingRedisServiceImpl — Implements temporary latest-location state caching in Redis.
 * Applies configurable TTL to entries and provides graceful fallback on connection issues.
 */
@Service
public class VehicleTrackingRedisServiceImpl implements VehicleTrackingRedisService {

    private static final Logger log = LoggerFactory.getLogger(VehicleTrackingRedisServiceImpl.class);
    private static final String KEY_PREFIX = "vehicle:";
    private static final String KEY_SUFFIX = ":latest-location";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisProperties redisProperties;

    public VehicleTrackingRedisServiceImpl(StringRedisTemplate stringRedisTemplate,
                                          ObjectMapper objectMapper,
                                          RedisProperties redisProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.redisProperties = redisProperties;
    }

    @Override
    public String buildLatestLocationKey(Long vehicleId) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle ID cannot be null for Redis key generation");
        }
        return KEY_PREFIX + vehicleId + KEY_SUFFIX;
    }

    @Override
    public void saveLatestLocation(VehicleLocationResponseDTO locationDTO) {
        if (locationDTO == null || locationDTO.getVehicleId() == null) {
            log.warn("Skipping Redis latest-location save: payload or vehicleId is null");
            return;
        }

        try {
            String key = buildLatestLocationKey(locationDTO.getVehicleId());
            String jsonPayload = objectMapper.writeValueAsString(locationDTO);
            long ttlSeconds = Math.max(1, redisProperties.getLatestLocationTtlSeconds());
            Duration ttl = Duration.ofSeconds(ttlSeconds);

            stringRedisTemplate.opsForValue().set(key, jsonPayload, ttl);
            log.debug("Cached latest location in Redis: key={} ttl={}s", key, ttlSeconds);
        } catch (Exception e) {
            log.warn("Failed to cache latest vehicle location in Redis (graceful degradation): {}", e.getMessage());
        }
    }

    @Override
    public void saveLatestLocation(Vehicle vehicle, VehicleLocation location) {
        if (vehicle == null || vehicle.getId() == null || location == null) {
            log.warn("Skipping Redis latest-location save: vehicle or location entity is null");
            return;
        }

        VehicleLocationResponseDTO dto = new VehicleLocationResponseDTO(
                location.getId(),
                vehicle.getId(),
                vehicle.getVehicleNumber(),
                location.getLatitude(),
                location.getLongitude(),
                location.getSpeed(),
                location.getHeading(),
                location.getRecordedAt()
        );

        saveLatestLocation(dto);
    }

    @Override
    public Optional<VehicleLocationResponseDTO> getLatestLocation(Long vehicleId) {
        if (vehicleId == null) {
            return Optional.empty();
        }

        try {
            String key = buildLatestLocationKey(vehicleId);
            String json = stringRedisTemplate.opsForValue().get(key);

            if (!StringUtils.hasText(json)) {
                log.debug("Redis cache miss for vehicle latest location: key={}", key);
                return Optional.empty();
            }

            VehicleLocationResponseDTO dto = objectMapper.readValue(json, VehicleLocationResponseDTO.class);
            log.debug("Redis cache hit for vehicle latest location: key={}", key);
            return Optional.of(dto);
        } catch (Exception e) {
            log.warn("Failed to retrieve latest location from Redis for vehicle ID {} (falling back to MySQL): {}",
                    vehicleId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void evictLatestLocation(Long vehicleId) {
        if (vehicleId == null) {
            return;
        }

        try {
            String key = buildLatestLocationKey(vehicleId);
            stringRedisTemplate.delete(key);
            log.debug("Evicted latest location from Redis: key={}", key);
        } catch (Exception e) {
            log.warn("Failed to evict latest location from Redis for vehicle ID {}: {}", vehicleId, e.getMessage());
        }
    }
}
