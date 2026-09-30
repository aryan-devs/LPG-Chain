package com.lpgchain.integration.traccar;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * TraccarClient — Interface defining the integration boundary with the external Traccar GPS tracking server.
 * Decouples LPG-Chain domain logic from HTTP communication and Traccar API specifications.
 */
public interface TraccarClient {

    /**
     * Retrieves device information from Traccar by device identifier or uniqueId.
     */
    Optional<TraccarDeviceDTO> getDevice(String deviceId);

    /**
     * Retrieves the latest GPS position recorded in Traccar for a given device.
     */
    Optional<TraccarPositionDTO> getLatestPosition(String deviceId);

    /**
     * Retrieves historical GPS positions recorded in Traccar for a given device across a time range.
     */
    List<TraccarPositionDTO> getDevicePositions(String deviceId, LocalDateTime from, LocalDateTime to);
}
