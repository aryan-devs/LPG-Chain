package com.lpgchain.integration.traccar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraccarClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private TraccarProperties properties;
    private TraccarClient traccarClient;

    @BeforeEach
    void setUp() {
        properties = new TraccarProperties();
        properties.setBaseUrl("http://localhost:8082");
        properties.setConnectTimeoutMillis(1000);
        properties.setReadTimeoutMillis(1000);

        traccarClient = new TraccarClientImpl(properties, restClient);
    }

    @Test
    @DisplayName("1. getLatestPosition: successful position retrieval")
    @SuppressWarnings("unchecked")
    void getLatestPosition_Success() {
        // ARRANGE
        TraccarPositionDTO position = new TraccarPositionDTO(1L, 10L, 18.6298, 73.7997, 32.5, 180.0, "2026-09-17T18:30:00Z");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(List.of(position));

        // ACT
        Optional<TraccarPositionDTO> result = traccarClient.getLatestPosition("784521");

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals(18.6298, result.get().getLatitude());
        assertEquals(73.7997, result.get().getLongitude());
        assertEquals(32.5, result.get().getSpeed());
    }

    @Test
    @DisplayName("2. getLatestPosition: Traccar server unavailable / timeout returns safe empty Optional")
    @SuppressWarnings("unchecked")
    void getLatestPosition_TraccarUnavailable_ReturnsEmpty() {
        // ARRANGE
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenThrow(new ResourceAccessException("I/O error: Connection refused"));

        // ACT
        Optional<TraccarPositionDTO> result = traccarClient.getLatestPosition("784521");

        // ASSERT
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("3. getLatestPosition: 5xx server error returns safe empty Optional without bubbling up")
    @SuppressWarnings("unchecked")
    void getLatestPosition_ServerError_ReturnsEmpty() {
        // ARRANGE
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenThrow(new RestClientResponseException(
                "Internal Server Error", 500, "Server Error", null, null, null));

        // ACT
        Optional<TraccarPositionDTO> result = traccarClient.getLatestPosition("784521");

        // ASSERT
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("4. getDevice: blank deviceId returns empty Optional immediately")
    void getDevice_BlankDeviceId_ReturnsEmpty() {
        // ACT
        Optional<TraccarDeviceDTO> result = traccarClient.getDevice("   ");

        // ASSERT
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("5. getDevice: successful device retrieval")
    @SuppressWarnings("unchecked")
    void getDevice_Success() {
        // ARRANGE
        TraccarDeviceDTO device = new TraccarDeviceDTO(10L, "Delivery Truck 1", "784521", "online", 1L);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(List.of(device));

        // ACT
        Optional<TraccarDeviceDTO> result = traccarClient.getDevice("784521");

        // ASSERT
        assertTrue(result.isPresent());
        assertEquals("784521", result.get().getUniqueId());
        assertEquals("online", result.get().getStatus());
    }

    @Test
    @DisplayName("6. getDevicePositions: network error returns empty list")
    @SuppressWarnings("unchecked")
    void getDevicePositions_NetworkError_ReturnsEmptyList() {
        // ARRANGE
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenThrow(new ResourceAccessException("Connection timed out"));

        // ACT
        List<TraccarPositionDTO> list = traccarClient.getDevicePositions("784521", LocalDateTime.now().minusHours(1), LocalDateTime.now());

        // ASSERT
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }
}
