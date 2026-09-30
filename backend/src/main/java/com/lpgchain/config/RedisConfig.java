package com.lpgchain.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lpgchain.service.VehicleTrackingRedisSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisConfig — Configures Spring Data Redis connectivity, explicit Jackson JSON serialization,
 * Pub/Sub channel topics, and asynchronous message listener containers.
 */
@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    /**
     * Pub/Sub channel topic bean for vehicle tracking event distribution.
     */
    @Bean
    public ChannelTopic trackingChannelTopic(RedisProperties redisProperties) {
        return new ChannelTopic(redisProperties.getTrackingChannel());
    }

    /**
     * Safe, explicit JSON-serialized RedisTemplate for general object caching.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       ObjectMapper defaultObjectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Configure Jackson ObjectMapper with JavaTimeModule for Java 8+ dates
        ObjectMapper redisObjectMapper = defaultObjectMapper.copy();
        redisObjectMapper.registerModule(new JavaTimeModule());
        redisObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        redisObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Jackson2JsonRedisSerializer<Object> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(redisObjectMapper, Object.class);

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    /**
     * Asynchronous message listener container for Redis Pub/Sub subscriber.
     * Manages background subscription tasks and reconnects on transient disconnects.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            VehicleTrackingRedisSubscriber subscriber,
            ChannelTopic trackingChannelTopic,
            RedisProperties redisProperties) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer() {
            @Override
            public boolean isAutoStartup() {
                return redisProperties.isListenerAutoStartup();
            }
        };
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, trackingChannelTopic);
        container.setRecoveryInterval(5000L);
        container.setErrorHandler(throwable ->
                log.error("Redis Pub/Sub message listener error: {}", throwable.getMessage(), throwable));

        log.info("RedisMessageListenerContainer initialized for channel [{}] (autoStartup={})",
                redisProperties.getTrackingChannel(), redisProperties.isListenerAutoStartup());
        return container;
    }
}
