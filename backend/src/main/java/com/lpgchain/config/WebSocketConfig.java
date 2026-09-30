package com.lpgchain.config;

import com.lpgchain.security.StompErrorHandler;
import com.lpgchain.security.WebSocketAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocketConfig — Configures Spring WebSocket with STOMP messaging for single-instance
 * real-time vehicle GPS telemetry broadcasting.
 *
 * Endpoint: /ws
 * Application Destination Prefix: /app
 * Message Broker Destination Prefix: /topic
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;
    private final StompErrorHandler stompErrorHandler;

    public WebSocketConfig(WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor,
                           StompErrorHandler stompErrorHandler) {
        this.webSocketAuthChannelInterceptor = webSocketAuthChannelInterceptor;
        this.stompErrorHandler = stompErrorHandler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory message broker carrying messages to clients on /topic prefixes
        registry.enableSimpleBroker("/topic");

        // Designate /app prefix for messages bound for @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register standard STOMP over WebSocket endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // Configure customized safe error handler
        registry.setErrorHandler(stompErrorHandler);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Intercept client inbound channel messages for JWT authentication and ReBAC subscription authorization
        registration.interceptors(webSocketAuthChannelInterceptor);
    }
}
