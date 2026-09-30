package com.lpgchain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lpgchain.config.RedisProperties;
import com.lpgchain.dto.VehicleLocationResponseDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.entity.VehicleLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleTrackingRedisServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private RedisProperties redisProperties;
    private VehicleTrackingRedisServiceImpl redisService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        redisProperties = new RedisProperties();
        redisProperties.setLatestLocationTtlSeconds(60);
        redisProperties.setTrackingChannel("lpgchain:tracking:vehicle-location");

        redisService = new VehicleTrackingRedisServiceImpl(stringRedisTemplate, objectMapper, redisProperties);
    }

    @Test
    @DisplayName("1. buildLatestLocationKey generates correct vehicle key structure")
    void buildLatestLocationKey_ValidId_GeneratesCorrectKey() {
        String key = redisService.buildLatestLocationKey(42L);
        assertEquals("vehicle:42:latest-location", key);
    }

    @Test
    @DisplayName("2. buildLatestLocationKey with null vehicleId throws IllegalArgumentException")
    void buildLatestLocationKey_NullId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> redisService.buildLatestLocationKey(null));
    }

    @Test
    @DisplayName("3. saveLatestLocation caches DTO in Redis with configured TTL")
    void saveLatestLocation_ValidDto_SavesWithTtl() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        LocalDateTime now = LocalDateTime.of(2026, 9, 29, 22, 30, 0);
        VehicleLocationResponseDTO dto = new VehicleLocationResponseDTO(
                101L, 5L, "MH-12-AB-1234", 18.5204, 73.8567, 45.0, 90.0, now
        );

        redisService.saveLatestLocation(dto);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq("vehicle:5:latest-location"), anyString(), ttlCaptor.capture());
        assertEquals(60, ttlCaptor.getValue().getSeconds());
    }

    @Test
    @DisplayName("4. saveLatestLocation from entity correctly constructs and saves DTO")
    void saveLatestLocation_FromEntities_ConstructsAndCachesDto() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        Distributor distributor = new Distributor();
        distributor.setId(1L);

        Vehicle vehicle = new Vehicle("MH-12-AB-1234", "LPG_DELIVERY", 30, "AVAILABLE", distributor);
        vehicle.setId(7L);

        LocalDateTime now = LocalDateTime.of(2026, 9, 29, 22, 30, 0);
        VehicleLocation location = new VehicleLocation(vehicle, 18.5204, 73.8567, 30.0, 180.0, now);
        location.setId(202L);

        redisService.saveLatestLocation(vehicle, location);

        verify(valueOperations).set(eq("vehicle:7:latest-location"), anyString(), eq(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("5. saveLatestLocation with null DTO or vehicleId handles gracefully without calling Redis")
    void saveLatestLocation_NullInput_GracefulNoOp() {
        redisService.saveLatestLocation((VehicleLocationResponseDTO) null);
        redisService.saveLatestLocation(new VehicleLocationResponseDTO());
        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("6. saveLatestLocation handles Redis connection errors gracefully without throwing")
    void saveLatestLocation_RedisThrowsException_HandledGracefully() {
        when(stringRedisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Connection refused"));

        VehicleLocationResponseDTO dto = new VehicleLocationResponseDTO(
                101L, 5L, "MH-12-AB-1234", 18.5204, 73.8567, 45.0, 90.0, LocalDateTime.now()
        );

        assertDoesNotThrow(() -> redisService.saveLatestLocation(dto));
    }

    @Test
    @DisplayName("7. getLatestLocation on cache hit returns deserialized VehicleLocationResponseDTO")
    void getLatestLocation_CacheHit_ReturnsDto() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        LocalDateTime recordedAt = LocalDateTime.of(2026, 9, 29, 22, 30, 0);
        VehicleLocationResponseDTO expectedDto = new VehicleLocationResponseDTO(
                505L, 10L, "MH-14-CD-5678", 18.6298, 73.7997, 28.5, 270.0, recordedAt
        );
        String jsonPayload = objectMapper.writeValueAsString(expectedDto);

        when(valueOperations.get("vehicle:10:latest-location")).thenReturn(jsonPayload);

        Optional<VehicleLocationResponseDTO> result = redisService.getLatestLocation(10L);

        assertTrue(result.isPresent());
        VehicleLocationResponseDTO actual = result.get();
        assertEquals(505L, actual.getId());
        assertEquals(10L, actual.getVehicleId());
        assertEquals("MH-14-CD-5678", actual.getVehicleNumber());
        assertEquals(18.6298, actual.getLatitude());
        assertEquals(73.7997, actual.getLongitude());
        assertEquals(28.5, actual.getSpeed());
        assertEquals(270.0, actual.getHeading());
        assertEquals(recordedAt, actual.getRecordedAt());
    }

    @Test
    @DisplayName("8. getLatestLocation on cache miss returns Optional.empty()")
    void getLatestLocation_CacheMiss_ReturnsEmpty() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("vehicle:10:latest-location")).thenReturn(null);

        Optional<VehicleLocationResponseDTO> result = redisService.getLatestLocation(10L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("9. getLatestLocation on Redis error returns Optional.empty() without throwing")
    void getLatestLocation_RedisError_ReturnsEmptyGracefully() {
        when(stringRedisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Timeout"));

        Optional<VehicleLocationResponseDTO> result = redisService.getLatestLocation(10L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("10. getLatestLocation with null vehicleId returns Optional.empty()")
    void getLatestLocation_NullVehicleId_ReturnsEmpty() {
        Optional<VehicleLocationResponseDTO> result = redisService.getLatestLocation(null);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("11. evictLatestLocation deletes key from Redis")
    void evictLatestLocation_ValidId_DeletesKey() {
        redisService.evictLatestLocation(15L);
        verify(stringRedisTemplate).delete("vehicle:15:latest-location");
    }

    @Test
    @DisplayName("12. evictLatestLocation handles Redis errors gracefully")
    void evictLatestLocation_RedisError_HandledGracefully() {
        when(stringRedisTemplate.delete(anyString())).thenThrow(new RedisConnectionFailureException("Down"));
        assertDoesNotThrow(() -> redisService.evictLatestLocation(15L));
    }
}
