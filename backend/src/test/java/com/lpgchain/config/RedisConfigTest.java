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
    @DisplayName("REDIS: Listener container initializes with bounded exponential backoff and non-blocking autoStartup=false")
    void testRedisMessageListenerContainerConfiguration() {
        RedisConfig config = new RedisConfig();
        RedisConnectionFactory mockFactory = mock(RedisConnectionFactory.class);
        VehicleTrackingEventPublisher mockPublisher = mock(VehicleTrackingEventPublisher.class);
        VehicleTrackingRedisSubscriber subscriber = new VehicleTrackingRedisSubscriber(new ObjectMapper(), mockPublisher);
        ChannelTopic topic = new ChannelTopic("lpgchain:tracking:vehicle-location");
        RedisProperties properties = new RedisProperties();
        properties.setListenerAutoStartup(true);

        RedisMessageListenerContainer container = config.redisMessageListenerContainer(
                mockFactory, subscriber, topic, properties);

        assertNotNull(container);
        assertFalse(container.isAutoStartup(), "Listener autoStartup MUST be false so Spring Boot never blocks Tomcat during context refresh");
    }

    @Test
    @DisplayName("REDIS: Async starter respects disabled configuration switch")
    void testAsyncStarterDisabledSwitch() {
        RedisConfig config = new RedisConfig();
        org.springframework.boot.context.event.ApplicationReadyEvent mockEvent =
                mock(org.springframework.boot.context.event.ApplicationReadyEvent.class);
        org.springframework.context.ConfigurableApplicationContext mockContext =
                mock(org.springframework.context.ConfigurableApplicationContext.class);

        RedisProperties disabledProps = new RedisProperties();
        disabledProps.setListenerAutoStartup(false);

        when(mockEvent.getApplicationContext()).thenReturn(mockContext);
        when(mockContext.getBean(RedisProperties.class)).thenReturn(disabledProps);

        assertDoesNotThrow(() -> config.startRedisListenerAsync(mockEvent));
        verify(mockContext, never()).getBean(RedisMessageListenerContainer.class);
    }
}
