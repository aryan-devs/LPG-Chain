package com.lpgchain.service;

import com.lpgchain.dto.VehicleLocationEventDTO;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.entity.VehicleLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * VehicleTrackingEventPublisher — Dedicated component responsible for constructing
 * and broadcasting real-time GPS tracking event DTOs over STOMP destinations.
 *
 * Encapsulates SimpMessagingTemplate to ensure controllers and entity services
 * do not directly depend on WebSocket messaging templates.
 */
@Service
public class VehicleTrackingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(VehicleTrackingEventPublisher.class);
    private static final String DELIVERY_TRACKING_TOPIC_PREFIX = "/topic/deliveries/";
    private static final String DELIVERY_TRACKING_TOPIC_SUFFIX = "/tracking";

    private final SimpMessagingTemplate messagingTemplate;

    public VehicleTrackingEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Constructs a VehicleLocationEventDTO from the persisted VehicleLocation and publishes
     * to the delivery-specific STOMP topic: /topic/deliveries/{deliveryId}/tracking.
     *
     * @param deliveryId the active delivery ID associated with the vehicle
     * @param location   the persisted vehicle location entity
     */
    public void publishLocationUpdate(Long deliveryId, VehicleLocation location) {
        if (deliveryId == null || location == null) {
            log.warn("Skipping tracking event publish: deliveryId or location is null");
            return;
        }

        Vehicle vehicle = location.getVehicle();
        VehicleLocationEventDTO event = new VehicleLocationEventDTO(
                deliveryId,
                vehicle != null ? vehicle.getId() : null,
                vehicle != null ? vehicle.getVehicleNumber() : null,
                location.getLatitude(),
                location.getLongitude(),
                location.getSpeed(),
                location.getHeading(),
                location.getRecordedAt()
        );

        publishLocationUpdate(deliveryId, event);
    }

    /**
     * Publishes a pre-constructed VehicleLocationEventDTO to the delivery-specific STOMP topic.
     *
     * @param deliveryId the active delivery ID
     * @param event      the event DTO
     */
    public void publishLocationUpdate(Long deliveryId, VehicleLocationEventDTO event) {
        if (deliveryId == null || event == null) {
            log.warn("Skipping tracking event publish: deliveryId or event is null");
            return;
        }

        String destination = buildDeliveryTopic(deliveryId);
        log.debug("Publishing vehicle location event for delivery #{} to destination {}", deliveryId, destination);
        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * Helper to construct the delivery tracking destination topic.
     */
    public static String buildDeliveryTopic(Long deliveryId) {
        return DELIVERY_TRACKING_TOPIC_PREFIX + deliveryId + DELIVERY_TRACKING_TOPIC_SUFFIX;
    }
}
