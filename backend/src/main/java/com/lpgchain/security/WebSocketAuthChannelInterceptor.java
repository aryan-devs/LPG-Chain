package com.lpgchain.security;

import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.exception.ResourceNotFoundException;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.service.DeliveryAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocketAuthChannelInterceptor — Intercepts inbound STOMP frames for:
 * 1. Authentication during CONNECT frame via JWT Bearer token.
 * 2. Strict ReBAC Authorization during SUBSCRIBE frame on /topic/deliveries/{deliveryId}/tracking.
 * 3. Prevention of arbitrary destination subscriptions and client GPS injection.
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_HEADER = "token";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final Pattern DELIVERY_TRACKING_PATTERN = Pattern.compile("^/topic/deliveries/(\\d+)/tracking$");

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryAuthorizationService deliveryAuthorizationService;

    public WebSocketAuthChannelInterceptor(JwtTokenProvider jwtTokenProvider,
                                           UserRepository userRepository,
                                           UserRoleRepository userRoleRepository,
                                           DeliveryRepository deliveryRepository,
                                           DeliveryAuthorizationService deliveryAuthorizationService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryAuthorizationService = deliveryAuthorizationService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        boolean wrapped = false;
        if (accessor == null || !accessor.isMutable()) {
            accessor = StompHeaderAccessor.wrap(message);
            accessor.setLeaveMutable(true);
            wrapped = true;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            handleSend(accessor);
        } else if (StompCommand.DISCONNECT.equals(command)) {
            handleDisconnect(accessor);
        }

        return wrapped ? MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders()) : message;
    }

    /**
     * Authenticates the STOMP connection using JWT token from native headers.
     * Extracts email and verified roles; attaches authenticated Principal to the session.
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        String token = resolveToken(accessor);

        if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket STOMP connection rejected: Missing or invalid JWT token");
            throw new AccessDeniedException("Unauthorized: Missing or invalid JWT token");
        }

        String email = jwtTokenProvider.getEmailFromToken(token);
        if (!StringUtils.hasText(email)) {
            log.warn("WebSocket STOMP connection rejected: No subject in token");
            throw new AccessDeniedException("Unauthorized: Invalid token subject");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            log.warn("WebSocket STOMP connection rejected: User not found in database for email: {}", email);
            throw new AccessDeniedException("Unauthorized: User not found");
        }

        User user = userOptional.get();
        String activeRoleClaim = jwtTokenProvider.getRoleFromToken(token);
        List<UserRole> userRoles = userRoleRepository.findByUser(user);

        List<GrantedAuthority> authorities;
        if (StringUtils.hasText(activeRoleClaim)) {
            String normalizedActiveRole = normalizeRole(activeRoleClaim);

            boolean roleStillAssigned = userRoles.stream()
                    .map(UserRole::getRole)
                    .filter(Objects::nonNull)
                    .map(Role::getName)
                    .filter(StringUtils::hasText)
                    .map(this::normalizeRole)
                    .anyMatch(normalizedActiveRole::equals);

            if (roleStillAssigned) {
                authorities = List.of(new SimpleGrantedAuthority(normalizedActiveRole));
            } else {
                authorities = Collections.emptyList();
            }
        } else {
            List<String> assignedNormalizedRoles = userRoles.stream()
                    .map(UserRole::getRole)
                    .filter(Objects::nonNull)
                    .map(Role::getName)
                    .filter(StringUtils::hasText)
                    .map(this::normalizeRole)
                    .distinct()
                    .toList();

            if (assignedNormalizedRoles.size() == 1) {
                authorities = List.of(new SimpleGrantedAuthority(assignedNormalizedRoles.get(0)));
            } else {
                authorities = Collections.emptyList();
            }
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);

        accessor.setUser(authentication);

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put("userId", user.getId());
            sessionAttributes.put("userEmail", user.getEmail());
        }

        log.info("WebSocket client authenticated successfully: user={}", user.getEmail());
    }

    /**
     * Enforces resource-level authorization (ReBAC) on subscription destinations.
     * Only allows subscriptions matching /topic/deliveries/{deliveryId}/tracking
     * where the user is authorized for that specific delivery.
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (!StringUtils.hasText(destination)) {
            throw new IllegalArgumentException("Subscription destination cannot be empty");
        }

        Matcher matcher = DELIVERY_TRACKING_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            log.warn("Subscription rejected: Attempt to subscribe to unauthorized arbitrary destination: {}", destination);
            throw new AccessDeniedException("Forbidden: Unauthorized subscription destination: " + destination);
        }

        Principal principal = accessor.getUser();
        if (!(principal instanceof Authentication auth) || !auth.isAuthenticated()) {
            log.warn("Subscription rejected: Unauthenticated user attempting to subscribe to {}", destination);
            throw new AccessDeniedException("Unauthorized: Authentication required to subscribe");
        }

        Long deliveryId;
        try {
            deliveryId = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid delivery ID in destination: " + destination);
        }

        Delivery delivery = deliveryRepository.findByIdWithDetails(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with ID: " + deliveryId));

        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            deliveryAuthorizationService.checkCanAccessDelivery(delivery);
            log.info("Subscription approved: user={} subscribed to delivery #{}", auth.getName(), deliveryId);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Prevents clients from sending tracking or GPS messages over WebSocket.
     */
    private void handleSend(StompHeaderAccessor accessor) {
        log.warn("Client message sending blocked on destination: {}", accessor.getDestination());
        throw new AccessDeniedException("Forbidden: Client message sending is not permitted on this WebSocket endpoint");
    }

    /**
     * Logs client disconnection safely without leaking sensitive information.
     */
    private void handleDisconnect(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        String user = principal != null ? principal.getName() : "unauthenticated";
        log.info("WebSocket STOMP client disconnected: user={}", user);
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader)) {
            String trimmed = authHeader.trim();
            if (trimmed.startsWith(BEARER_PREFIX)) {
                return trimmed.substring(BEARER_PREFIX.length()).trim();
            }
            return trimmed;
        }

        String tokenHeader = accessor.getFirstNativeHeader(TOKEN_HEADER);
        if (StringUtils.hasText(tokenHeader)) {
            return tokenHeader.trim();
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey(TOKEN_HEADER)) {
            Object tokenObj = sessionAttributes.get(TOKEN_HEADER);
            if (tokenObj != null) {
                return tokenObj.toString().trim();
            }
        }

        return null;
    }

    private String normalizeRole(String roleName) {
        String trimmed = roleName.trim().toUpperCase();
        if (trimmed.startsWith("ROLE_")) {
            return trimmed;
        }
        return "ROLE_" + trimmed;
    }
}
