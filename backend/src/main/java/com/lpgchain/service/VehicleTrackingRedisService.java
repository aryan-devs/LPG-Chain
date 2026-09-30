package com.lpgchain.service;

import com.lpgchain.dto.VehicleLocationResponseDTO;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.entity.VehicleLocation;

import java.util.Optional;

/**
 * VehicleTrackingRedisService — Contract for caching and retrieving fast-changing
 * latest vehicle GPS telemetry in Redis with TTL expiration.
 */
public interface VehicleTrackingRedisService {

    /**
     * Builds the standard Redis key for a vehicle's latest location cache entry.
     * Format: vehicle:{vehicleId}:latest-location
     *
     * @param vehicleId the vehicle identifier
     * @return predictable Redis key
     */
    String buildLatestLocationKey(Long vehicleId);

    /**
     * Caches the latest vehicle location in Redis with configured TTL.
     * Refreshes the TTL on every write.
     *
     * @param locationDTO the latest vehicle location DTO
     */
    void saveLatestLocation(VehicleLocationResponseDTO locationDTO);

    /**
     * Convenience method to cache the latest location from domain entities.
     *
     * @param vehicle  the vehicle entity
     * @param location the persisted location entity
     */
    void saveLatestLocation(Vehicle vehicle, VehicleLocation location);

    /**
     * Retrieves the latest cached vehicle location from Redis.
     *
     * @param vehicleId the vehicle identifier
     * @return Optional containing the cached location if present, or Optional.empty() on cache miss or Redis error
     */
    Optional<VehicleLocationResponseDTO> getLatestLocation(Long vehicleId);

    /**
     * Evicts the cached latest location for the vehicle.
     *
     * @param vehicleId the vehicle identifier
     */
    void evictLatestLocation(Long vehicleId);
}
