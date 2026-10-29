package com.lpgchain.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lpgchain.service.VehicleTrackingRedisSubscriber;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * RedisConfig — Configures Spring Data Redis connectivity, explicit Jackson JSON serialization,
 * Pub/Sub channel topics, and asynchronous message listener containers.
 *
 * Designed for graceful degradation: if Redis is temporarily unreachable or misconfigured in production,
 * core authentication, database transactions, and APIs remain 100% operational.
 */
@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    /**
     * Configures the RedisConnectionFactory.
     * Supports cloud deployments using REDIS_URL with TLS (e.g. rediss://... on Render / Upstash)
     * while preserving standalone host/port/password configuration for local development.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.url:${REDIS_URL:}}") String redisUrl,
            @Value("${spring.data.redis.host:${REDIS_HOST:localhost}}") String host,
            @Value("${spring.data.redis.port:${REDIS_PORT:6379}}") int port,
            @Value("${spring.data.redis.password:${REDIS_PASSWORD:}}") String password,
            @Value("${spring.data.redis.timeout:${REDIS_TIMEOUT:10000}}") long timeoutMillis) {

        Duration timeout = Duration.ofMillis(timeoutMillis > 0 ? timeoutMillis : 10000);

        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofSeconds(10))
                .keepAlive(true)
                .build();

        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build();

        if (redisUrl != null && !redisUrl.trim().isEmpty()) {
            String trimmedUrl = redisUrl.trim();
            RedisURI redisUri = RedisURI.create(trimmedUrl);
            redisUri.setTimeout(timeout);

            LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
                    LettuceClientConfiguration.builder()
                            .commandTimeout(timeout)
                            .clientOptions(clientOptions);

            boolean useSsl = trimmedUrl.startsWith("rediss://") || redisUri.isSsl();
            if (useSsl) {
                clientConfigBuilder.useSsl();
            }

            RedisConfiguration redisConfig = LettuceConnectionFactory.createRedisConfiguration(redisUri);
            log.info("Configuring LettuceConnectionFactory with REDIS_URL [host={}, port={}, ssl={}, commandTimeout={}ms]",
                    redisUri.getHost(), redisUri.getPort(), useSsl, timeout.toMillis());

            LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfigBuilder.build());
            // Do not block application context startup if cloud Redis handshake is slow
            factory.setValidateConnection(false);
            return factory;
        }

        // Standalone fallback: local development or containerized Redis
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.trim().isEmpty()) {
            standaloneConfig.setPassword(RedisPassword.of(password.trim()));
        }

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(timeout)
                .clientOptions(clientOptions)
                .build();

        log.info("Configuring LettuceConnectionFactory with standalone host [{}:{}]", host, port);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig, clientConfig);
        factory.setValidateConnection(false);
        return factory;
    }

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
     * Configured with autoStartup=false so Spring's DefaultLifecycleProcessor NEVER blocks
     * embedded Tomcat or context refresh on Redis connectivity.
     * Bounded exponential backoff and non-blocking recovery ensure robust fault-tolerance.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            VehicleTrackingRedisSubscriber subscriber,
            ChannelTopic trackingChannelTopic,
            RedisProperties redisProperties) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer() {
            private int consecutiveFailures = 0;

            @Override
            public boolean isAutoStartup() {
                // Always false during Spring context refresh to guarantee Tomcat starts and
                // opens the server port immediately without blocking on cloud Redis handshakes.
                return false;
            }

            @Override
            protected void handleSubscriptionException(CompletableFuture<Void> future, BackOffExecution backoff, Throwable ex) {
                consecutiveFailures++;
                // Throttle logs: log concise warnings without flooding Render with massive multi-page stack traces
                if (consecutiveFailures <= 3 || consecutiveFailures % 10 == 0) {
                    log.warn("[REDIS-DEGRADED] Redis Pub/Sub subscription attempt #{} failed: {}. Recovery scheduled by backoff. Core database & auth remain fully operational.",
                            consecutiveFailures, ex.getMessage());
                } else {
                    log.debug("[REDIS-DEGRADED] Redis Pub/Sub reconnect attempt #{} failed: {}", consecutiveFailures, ex.getMessage());
                }

                // Delegate to superclass which schedules recovery asynchronously via task executor without blocking
                super.handleSubscriptionException(future, backoff, ex);
            }
        };

        // Bounded exponential backoff: starts at 5s, grows by 1.5x up to 60s max interval
        ExponentialBackOff backOff = new ExponentialBackOff(5000L, 1.5);
        backOff.setMaxInterval(60000L);

        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, trackingChannelTopic);
        container.setRecoveryBackoff(backOff);
        container.setErrorHandler(throwable ->
                log.warn("[REDIS-DEGRADED] Redis Pub/Sub message execution error: {}", throwable.getMessage()));

        log.info("RedisMessageListenerContainer initialized for channel [{}] (autoStartup=false for non-blocking server start)",
                redisProperties.getTrackingChannel());
        return container;
    }

    /**
     * Non-blocking background starter: kicks off Redis Pub/Sub listener ONLY after Tomcat
     * is already listening on the assigned port and Spring Boot application is fully ready.
     */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void startRedisListenerAsync(org.springframework.boot.context.event.ApplicationReadyEvent event) {
        RedisProperties properties = event.getApplicationContext().getBean(RedisProperties.class);
        if (!properties.isListenerAutoStartup()) {
            log.info("Redis listener auto-startup disabled by configuration (lpgchain.redis.listener-auto-startup=false). Skipping background start.");
            return;
        }

        RedisMessageListenerContainer container = event.getApplicationContext().getBean(RedisMessageListenerContainer.class);

        Thread starterThread = new Thread(() -> {
            long delay = 5000L;
            int attempt = 1;
            while (attempt <= 10 && !container.isRunning()) {
                try {
                    log.info("Starting RedisMessageListenerContainer asynchronously in background (attempt #{})...", attempt);
                    container.start();
                    log.info("RedisMessageListenerContainer started successfully in background.");
                    return;
                } catch (Exception ex) {
                    log.warn("[REDIS-DEGRADED] Redis Pub/Sub background listener attempt #{} could not connect: {}. Retrying in {}s. (Core database & auth remain fully operational).",
                            attempt, ex.getMessage(), delay / 1000);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    delay = Math.min((long) (delay * 1.5), 60000L);
                    attempt++;
                }
            }
            if (!container.isRunning()) {
                log.warn("[REDIS-DEGRADED] Redis Pub/Sub background listener ceased retry after 10 attempts. Live tracking remains degraded. Core database and auth remain fully operational.");
            }
        }, "redis-listener-async-starter");

        starterThread.setDaemon(true);
        starterThread.start();
    }
}
