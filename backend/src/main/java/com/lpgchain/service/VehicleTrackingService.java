package com.lpgchain.service;

import com.lpgchain.dto.VehicleLocationRequestDTO;
import com.lpgchain.dto.VehicleLocationResponseDTO;

import java.util.List;
import java.util.Optional;

/**
 * VehicleTrackingService — Interface defining GPS tracking operations, location ingestion,
 * history queries, and Traccar device synchronization.
 */
public interface VehicleTrackingService {

    /**
     * Records a new GPS telemetry location for the specified vehicle.
     * Validates GPS data, checks vehicle existence and authorization.
     */
    VehicleLocationResponseDTO recordLocation(Long vehicleId, VehicleLocationRequestDTO requestDTO);

    /**
     * Retrieves the latest recorded GPS location for a vehicle.
     */
    Optional<VehicleLocationResponseDTO> getLatestLocation(Long vehicleId);

    /**
     * Retrieves location history for a vehicle ordered by recordedAt descending with limit.
     */
    List<VehicleLocationResponseDTO> getLocationHistory(Long vehicleId, int limit);

    /**
     * Retrieves the latest location of the vehicle assigned to the specified delivery.
     * Enforces customer ReBAC (customers may only view their own delivery).
     */
    Optional<VehicleLocationResponseDTO> getLatestLocationByDeliveryId(Long deliveryId);

    /**
     * Maps or updates the Traccar device ID for a vehicle.
     */
    void setTraccarDeviceId(Long vehicleId, String traccarDeviceId);

    /**
     * Retrieves the Traccar device ID for a vehicle.
     */
    Optional<String> getTraccarDeviceId(Long vehicleId);

    /**
     * Synchronizes the latest position from the external Traccar server for the vehicle.
     */
    Optional<VehicleLocationResponseDTO> syncLocationFromTraccar(Long vehicleId);
}
