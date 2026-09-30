package com.lpgchain.integration.traccar;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Traccar GPS tracking integration.
 * Defaults ensure zero startup failure when an external Traccar server is not running.
 */
@Component
@ConfigurationProperties(prefix = "traccar")
public class TraccarProperties {

    /**
     * Base URL for the Traccar server API (e.g. http://localhost:8082).
     */
    private String baseUrl = "http://localhost:8082";

    /**
     * Connection timeout in milliseconds.
     */
    private int connectTimeoutMillis = 3000;

    /**
     * Read timeout in milliseconds.
     */
    private int readTimeoutMillis = 3000;

    /**
     * Optional API token for Traccar authentication.
     */
    private String apiToken;

    /**
     * Optional username for Basic Auth.
     */
    private String username;

    /**
     * Optional password for Basic Auth.
     */
    private String password;

    public TraccarProperties() {
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
