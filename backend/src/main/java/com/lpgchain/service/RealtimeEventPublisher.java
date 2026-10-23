package com.lpgchain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * RealtimeEventPublisher — Broadcasts server-side business notifications to
 * authorized WebSocket subscribers (Customer, Distributor, Admin).
 */
@Service
public class RealtimeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishCustomerEvent(Long customerId, String eventType, Object payload) {
        if (customerId == null) return;
        try {
            String destination = "/topic/customers/" + customerId + "/events";
            messagingTemplate.convertAndSend(destination, Map.of(
                    "type", eventType,
                    "timestamp", System.currentTimeMillis(),
                    "data", payload
            ));
            log.debug("Sent customer event to {}: {}", destination, eventType);
        } catch (Exception e) {
            log.error("Failed to broadcast customer event: {}", e.getMessage());
        }
    }

    public void publishDistributorEvent(Long distributorId, String eventType, Object payload) {
        if (distributorId == null) return;
        try {
            String destination = "/topic/distributors/" + distributorId + "/events";
            messagingTemplate.convertAndSend(destination, Map.of(
                    "type", eventType,
                    "timestamp", System.currentTimeMillis(),
                    "data", payload
            ));
            log.debug("Sent distributor event to {}: {}", destination, eventType);
        } catch (Exception e) {
            log.error("Failed to broadcast distributor event: {}", e.getMessage());
        }
    }

    public void publishAdminEvent(String eventType, Object payload) {
        try {
            String destination = "/topic/admin/events";
            messagingTemplate.convertAndSend(destination, Map.of(
                    "type", eventType,
                    "timestamp", System.currentTimeMillis(),
                    "data", payload
            ));
            log.debug("Sent admin event to {}: {}", destination, eventType);
        } catch (Exception e) {
            log.error("Failed to broadcast admin event: {}", e.getMessage());
        }
    }
}
