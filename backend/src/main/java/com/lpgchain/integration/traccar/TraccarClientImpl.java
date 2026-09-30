package com.lpgchain.integration.traccar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * TraccarClientImpl — Production implementation of TraccarClient using Spring's RestClient.
 * Safely handles connection failures, timeouts, and HTTP errors without disrupting application startup or runtime.
 */
@Service
public class TraccarClientImpl implements TraccarClient {

    private static final Logger log = LoggerFactory.getLogger(TraccarClientImpl.class);

    private final TraccarProperties properties;
    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public TraccarClientImpl(TraccarProperties properties) {
        this(properties, buildRestClient(properties));
    }

    public TraccarClientImpl(TraccarProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    private static RestClient buildRestClient(TraccarProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(properties.getReadTimeoutMillis());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory);

        if (StringUtils.hasText(properties.getApiToken())) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiToken().trim());
        } else if (StringUtils.hasText(properties.getUsername()) && properties.getPassword() != null) {
            String credentials = properties.getUsername().trim() + ":" + properties.getPassword();
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        }

        return builder.build();
    }

    @Override
    public Optional<TraccarDeviceDTO> getDevice(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return Optional.empty();
        }

        try {
            List<TraccarDeviceDTO> devices = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/devices")
                            .queryParam("uniqueId", deviceId.trim())
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<TraccarDeviceDTO>>() {});

            if (devices != null && !devices.isEmpty()) {
                return Optional.ofNullable(devices.get(0));
            }
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Traccar getDevice failed for deviceId {}: {}", deviceId, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<TraccarPositionDTO> getLatestPosition(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return Optional.empty();
        }

        try {
            List<TraccarPositionDTO> positions = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/positions")
                            .queryParam("deviceId", deviceId.trim())
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<TraccarPositionDTO>>() {});

            if (positions != null && !positions.isEmpty()) {
                return Optional.ofNullable(positions.get(0));
            }
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Traccar getLatestPosition failed for deviceId {}: {}", deviceId, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<TraccarPositionDTO> getDevicePositions(String deviceId, LocalDateTime from, LocalDateTime to) {
        if (!StringUtils.hasText(deviceId)) {
            return Collections.emptyList();
        }

        try {
            List<TraccarPositionDTO> positions = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/api/positions")
                                .queryParam("deviceId", deviceId.trim());
                        if (from != null) {
                            builder.queryParam("from", from.format(DateTimeFormatter.ISO_DATE_TIME));
                        }
                        if (to != null) {
                            builder.queryParam("to", to.format(DateTimeFormatter.ISO_DATE_TIME));
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<TraccarPositionDTO>>() {});

            return positions != null ? positions : Collections.emptyList();
        } catch (Exception ex) {
            log.warn("Traccar getDevicePositions failed for deviceId {}: {}", deviceId, ex.getMessage());
            return Collections.emptyList();
        }
    }
}
