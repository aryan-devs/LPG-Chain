package com.lpgchain.security;

import com.lpgchain.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

/**
 * StompErrorHandler — Handles STOMP messaging exceptions and converts them into
 * safe, sanitized STOMP ERROR frames.
 *
 * Prevents leakage of internal stack traces, database schema, or infrastructure details.
 */
@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(StompErrorHandler.class);

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        Throwable rootCause = findRootCause(ex);
        String errorMessage;

        if (rootCause instanceof AccessDeniedException) {
            errorMessage = rootCause.getMessage() != null ? rootCause.getMessage() : "Forbidden: Access denied";
            log.warn("STOMP Access Denied: {}", errorMessage);
        } else if (rootCause instanceof AuthenticationException) {
            errorMessage = rootCause.getMessage() != null ? rootCause.getMessage() : "Unauthorized: Authentication required";
            log.warn("STOMP Authentication Failure: {}", errorMessage);
        } else if (rootCause instanceof ResourceNotFoundException) {
            errorMessage = rootCause.getMessage();
            log.warn("STOMP Resource Not Found: {}", errorMessage);
        } else if (rootCause instanceof IllegalArgumentException) {
            errorMessage = rootCause.getMessage();
            log.warn("STOMP Bad Request: {}", errorMessage);
        } else {
            errorMessage = "Message processing error";
            log.error("STOMP Unexpected Error: {}", rootCause.getMessage());
        }

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(errorMessage);
        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(errorMessage.getBytes(StandardCharsets.UTF_8), accessor.getMessageHeaders());
    }

    private Throwable findRootCause(Throwable throwable) {
        if (throwable == null) {
            return new RuntimeException("Unknown error");
        }
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }
}
