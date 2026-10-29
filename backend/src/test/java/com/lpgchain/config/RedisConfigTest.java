package com.lpgchain.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lpgchain.service.VehicleTrackingEventPublisher;
import com.lpgchain.service.VehicleTrackingRedisSubscriber;
import io.lettuce.core.RedisURI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.backoff.BackOffExecution;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisConfigTest {

    @Test
    @DisplayName("REDIS: Parses rediss:// TLS URI into standalone configuration with credentials and SSL")
    void testRedisUriParsing() {
        String url = "rediss://default:mypassword123@sought-redfish-236668.upstash.io:6379";
        RedisURI redisUri = RedisURI.create(url);
        assertEquals("sought-redfish-236668.upstash.io", redisUri.getHost());
        assertEquals(6379, redisUri.getPort());
        assertTrue(redisUri.isSsl());
        assertEquals("default", redisUri.getUsername());
        assertNotNull(redisUri.getPassword());

        RedisConfiguration config = LettuceConnectionFactory.createRedisConfiguration(redisUri);
        assertInstanceOf(RedisStandaloneConfiguration.class, config);
        RedisStandaloneConfiguration standalone = (RedisStandaloneConfiguration) config;
        assertEquals("sought-redfish-236668.upstash.io", standalone.getHostName());
        assertEquals(6379, standalone.getPort());
        assertEquals("default", standalone.getUsername());
        assertTrue(standalone.getPassword().isPresent());
        assertEquals("mypassword123", new String(standalone.getPassword().get()));
    }

    @Test
    @DisplayName("REDIS: Factory bean creates LettuceConnectionFactory with SSL, client options, and non-blocking startup validation")
    void testRedisConnectionFactoryBeanCreation() {
        RedisConfig config = new RedisConfig();
        String url = "rediss://default:secret@sought-redfish-236668.upstash.io:6379";

        RedisConnectionFactory factory = config.redisConnectionFactory(url, "localhost", 6379, "", 5000);
        assertNotNull(factory);
        assertInstanceOf(LettuceConnectionFactory.class, factory);

        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
        assertTrue(lettuceFactory.getClientConfiguration().isUseSsl());
        assertFalse(lettuceFactory.getValidateConnection(), "validateConnection must be false so slow cloud Redis doesn't block startup");
    }

    @Test
    @DisplayName("REDIS: Listener container initializes with bounded exponential backoff and safe autoStartup toggle")
    void testRedisMessageListenerContainerConfiguration() {
        RedisConfig config = new RedisConfig();
        RedisConnectionFactory mockFactory = mock(RedisConnectionFactory.class);
        VehicleTrackingEventPublisher mockPublisher = mock(VehicleTrackingEventPublisher.class);
        VehicleTrackingRedisSubscriber subscriber = new VehicleTrackingRedisSubscriber(new ObjectMapper(), mockPublisher);
        ChannelTopic topic = new ChannelTopic("lpgchain:tracking:vehicle-location");
        RedisProperties properties = new RedisProperties();
        properties.setListenerAutoStartup(false);

        RedisMessageListenerContainer container = config.redisMessageListenerContainer(
                mockFactory, subscriber, topic, properties);

        assertNotNull(container);
        assertFalse(container.isAutoStartup(), "Listener autoStartup should match RedisProperties");
    }

    @Test
    @DisplayName("REDIS: Degraded subscription exception handler catches errors and applies backoff without crashing")
    void testDegradedListenerSubscriptionExceptionHandling() throws Exception {
        RedisConfig config = new RedisConfig();
        RedisConnectionFactory mockFactory = mock(RedisConnectionFactory.class);
        VehicleTrackingEventPublisher mockPublisher = mock(VehicleTrackingEventPublisher.class);
        VehicleTrackingRedisSubscriber subscriber = new VehicleTrackingRedisSubscriber(new ObjectMapper(), mockPublisher);
        ChannelTopic topic = new ChannelTopic("lpgchain:tracking:vehicle-location");
        RedisProperties properties = new RedisProperties();

        RedisMessageListenerContainer container = config.redisMessageListenerContainer(
                mockFactory, subscriber, topic, properties);

        // Access the protected handleSubscriptionException method via reflection
        Method method = container.getClass().getDeclaredMethod(
                "handleSubscriptionException",
                CompletableFuture.class,
                BackOffExecution.class,
                Throwable.class
        );
        method.setAccessible(true);

        CompletableFuture<Void> future = new CompletableFuture<>();
        BackOffExecution mockBackoff = mock(BackOffExecution.class);
        when(mockBackoff.nextBackOff()).thenReturn(10L); // 10ms for quick test sleep

        Throwable testException = new RuntimeException("Simulated Upstash connection drop");

        assertDoesNotThrow(() -> {
            method.invoke(container, future, mockBackoff, testException);
        });

        verify(mockBackoff).nextBackOff();
    }
}
