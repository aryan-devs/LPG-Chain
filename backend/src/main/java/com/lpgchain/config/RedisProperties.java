package com.lpgchain.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RedisProperties — Externalized configuration properties for LPG-Chain Redis integration.
 * Supports configurable TTL for fast-changing tracking state, Pub/Sub channel names,
 * and test isolation switches.
 */
@Component
@ConfigurationProperties(prefix = "lpgchain.redis")
public class RedisProperties {

    /**
     * Time-to-live in seconds for latest vehicle location cache entries.
     * Default: 60 seconds (represents fast-changing tracking state).
     */
    private long latestLocationTtlSeconds = 60;

    /**
     * Redis Pub/Sub topic channel for broadcasting vehicle location tracking events.
     * Default: lpgchain:tracking:vehicle-location
     */
    private String trackingChannel = "lpgchain:tracking:vehicle-location";

    /**
     * Controls whether the Redis Pub/Sub message listener container should auto-start on context refresh.
     * Defaults to true in production/dev; set to false in test environments when no Redis instance is running.
     */
    private boolean listenerAutoStartup = true;

    /**
     * Unique identifier for this application instance (e.g. lpg-chain-1, lpg-chain-2).
     * Useful for tracking logs, debugging, and multi-instance observability.
     */
    private String instanceId = "lpg-chain-1";

    public RedisProperties() {
    }

    public long getLatestLocationTtlSeconds() {
        return latestLocationTtlSeconds;
    }

    public void setLatestLocationTtlSeconds(long latestLocationTtlSeconds) {
        this.latestLocationTtlSeconds = latestLocationTtlSeconds;
    }

    public String getTrackingChannel() {
        return trackingChannel;
    }

    public void setTrackingChannel(String trackingChannel) {
        this.trackingChannel = trackingChannel;
    }

    public boolean isListenerAutoStartup() {
        return listenerAutoStartup;
    }

    public void setListenerAutoStartup(boolean listenerAutoStartup) {
        this.listenerAutoStartup = listenerAutoStartup;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}
