package com.lpgchain.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.junit.jupiter.api.Assertions.*;

class RedisConfigTest {

    private final RedisConfig redisConfig = new RedisConfig();

    @Test
    @DisplayName("Should create standalone LettuceConnectionFactory when REDIS_URL is empty")
    void testStandaloneFallback() {
        RedisConnectionFactory factory = redisConfig.redisConnectionFactory(
                "",
                "localhost",
                6379,
                "",
                2000
        );

        assertNotNull(factory);
        assertTrue(factory instanceof LettuceConnectionFactory);
        LettuceConnectionFactory lettuce = (LettuceConnectionFactory) factory;
        assertEquals("localhost", lettuce.getHostName());
        assertEquals(6379, lettuce.getPort());
        assertFalse(lettuce.getClientConfiguration().isUseSsl());
    }

    @Test
    @DisplayName("Should configure TLS connection when REDIS_URL uses rediss:// scheme (e.g. Upstash)")
    void testUpstashTlsRedisUrl() {
        String upstashUrl = "rediss://default:testSecretToken123@sought-redfish-236668.upstash.io:6379";
        RedisConnectionFactory factory = redisConfig.redisConnectionFactory(
                upstashUrl,
                "localhost",
                6379,
                "",
                2000
        );

        assertNotNull(factory);
        assertTrue(factory instanceof LettuceConnectionFactory);
        LettuceConnectionFactory lettuce = (LettuceConnectionFactory) factory;
        assertEquals("sought-redfish-236668.upstash.io", lettuce.getHostName());
        assertEquals(6379, lettuce.getPort());
        assertTrue(lettuce.getClientConfiguration().isUseSsl(), "Lettuce connection must have SSL enabled for rediss:// URLs");
    }

    @Test
    @DisplayName("Should configure standard connection when REDIS_URL uses redis:// scheme")
    void testStandardRedisUrl() {
        String plainUrl = "redis://:mypassword@cache.internal:6380";
        RedisConnectionFactory factory = redisConfig.redisConnectionFactory(
                plainUrl,
                "localhost",
                6379,
                "",
                2000
        );

        assertNotNull(factory);
        assertTrue(factory instanceof LettuceConnectionFactory);
        LettuceConnectionFactory lettuce = (LettuceConnectionFactory) factory;
        assertEquals("cache.internal", lettuce.getHostName());
        assertEquals(6380, lettuce.getPort());
        assertFalse(lettuce.getClientConfiguration().isUseSsl());
    }
}
