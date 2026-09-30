package com.lpgchain.security;

import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.exception.ResourceNotFoundException;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class WebSocketTrackingSecurityIntegrationTest {

    @Autowired
    private WebSocketAuthChannelInterceptor interceptor;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserRoleRepository userRoleRepository;

    @MockBean
    private DistributorRepository distributorRepository;

    @MockBean
    private DistributorMembershipRepository distributorMembershipRepository;

    @MockBean
    private DeliveryRepository deliveryRepository;

    @MockBean
    private VehicleRepository vehicleRepository;

    private User adminUser;
    private User customerUserA;
    private User customerUserB;
    private User distributorUserOrg1;
    private User distributorUserOrg2;

    private Distributor distributorOrg1;
    private Distributor distributorOrg2;

    private Delivery deliveryA;
    private Delivery deliveryB;

    private String adminToken;
    private String customerAToken;
    private String customerBToken;
    private String distributor1Token;
    private String distributor2Token;

    private final MessageChannel mockChannel = mock(MessageChannel.class);

    @BeforeEach
    void setUp() {
        Role adminRole = new Role(1L, "ADMIN");
        Role customerRole = new Role(2L, "CUSTOMER");
        Role distributorRole = new Role(3L, "DISTRIBUTOR");

        // Admin User
        adminUser = new User(100L, "Admin User", "admin@lpgchain.example", "hash");
        when(userRepository.findByEmail("admin@lpgchain.example")).thenReturn(Optional.of(adminUser));
        when(userRoleRepository.findByUser(adminUser)).thenReturn(List.of(new UserRole(1L, adminUser, adminRole)));
        adminToken = jwtTokenProvider.generateToken(100L, "admin@lpgchain.example", "ADMIN");

        // Customer User A
        customerUserA = new User(1L, "Customer A", "customera@gmail.com", "hash");
        when(userRepository.findByEmail("customera@gmail.com")).thenReturn(Optional.of(customerUserA));
        when(userRoleRepository.findByUser(customerUserA)).thenReturn(List.of(new UserRole(2L, customerUserA, customerRole)));
        customerAToken = jwtTokenProvider.generateToken(1L, "customera@gmail.com", "CUSTOMER");

        // Customer User B
        customerUserB = new User(2L, "Customer B", "customerb@gmail.com", "hash");
        when(userRepository.findByEmail("customerb@gmail.com")).thenReturn(Optional.of(customerUserB));
        when(userRoleRepository.findByUser(customerUserB)).thenReturn(List.of(new UserRole(3L, customerUserB, customerRole)));
        customerBToken = jwtTokenProvider.generateToken(2L, "customerb@gmail.com", "CUSTOMER");

        // Distributor Org 1 & User
        distributorOrg1 = new Distributor();
        distributorOrg1.setId(10L);
        distributorOrg1.setAgencyName("Distributor Org 1");

        distributorUserOrg1 = new User(10L, "Dist 1 User", "dist1@approved-distributor.example", "hash");
        when(userRepository.findByEmail("dist1@approved-distributor.example")).thenReturn(Optional.of(distributorUserOrg1));
        when(userRoleRepository.findByUser(distributorUserOrg1)).thenReturn(List.of(new UserRole(4L, distributorUserOrg1, distributorRole)));
        distributor1Token = jwtTokenProvider.generateToken(10L, "dist1@approved-distributor.example", "DISTRIBUTOR");

        DistributorMembership membership1 = new DistributorMembership(distributorUserOrg1, distributorOrg1, "MANAGER", "ACTIVE");
        membership1.setId(1L);
        when(distributorMembershipRepository.findByUserId(10L)).thenReturn(List.of(membership1));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(10L, 10L)).thenReturn(Optional.of(membership1));

        // Distributor Org 2 & User
        distributorOrg2 = new Distributor();
        distributorOrg2.setId(20L);
        distributorOrg2.setAgencyName("Distributor Org 2");

        distributorUserOrg2 = new User(20L, "Dist 2 User", "dist2@approved-distributor.example", "hash");
        when(userRepository.findByEmail("dist2@approved-distributor.example")).thenReturn(Optional.of(distributorUserOrg2));
        when(userRoleRepository.findByUser(distributorUserOrg2)).thenReturn(List.of(new UserRole(5L, distributorUserOrg2, distributorRole)));
        distributor2Token = jwtTokenProvider.generateToken(20L, "dist2@approved-distributor.example", "DISTRIBUTOR");

        DistributorMembership membership2 = new DistributorMembership(distributorUserOrg2, distributorOrg2, "MANAGER", "ACTIVE");
        membership2.setId(2L);
        when(distributorMembershipRepository.findByUserId(20L)).thenReturn(List.of(membership2));
        when(distributorMembershipRepository.findByUserIdAndDistributorId(20L, 20L)).thenReturn(Optional.of(membership2));

        // Vehicles
        Vehicle vehicle1 = new Vehicle("MH-01-AB-1234", "LPG_DELIVERY", 30, "AVAILABLE", distributorOrg1);
        vehicle1.setId(101L);
        Vehicle vehicle2 = new Vehicle("MH-02-CD-5678", "LPG_DELIVERY", 30, "AVAILABLE", distributorOrg2);
        vehicle2.setId(202L);

        // Bookings & Deliveries
        Booking bookingA = new Booking();
        bookingA.setId(1001L);
        bookingA.setCustomer(customerUserA);

        deliveryA = new Delivery(bookingA, null, vehicle1, distributorOrg1, "ASSIGNED");
        deliveryA.setId(501L);
        when(deliveryRepository.findById(501L)).thenReturn(Optional.of(deliveryA));
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(deliveryA));

        Booking bookingB = new Booking();
        bookingB.setId(1002L);
        bookingB.setCustomer(customerUserB);

        deliveryB = new Delivery(bookingB, null, vehicle2, distributorOrg2, "ASSIGNED");
        deliveryB.setId(502L);
        when(deliveryRepository.findById(502L)).thenReturn(Optional.of(deliveryB));
        when(deliveryRepository.findByIdWithDetails(502L)).thenReturn(Optional.of(deliveryB));
    }

    private UsernamePasswordAuthenticationToken authenticateConnect(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        if (token != null) {
            accessor.addNativeHeader("Authorization", "Bearer " + token);
        }
        accessor.setSessionAttributes(new HashMap<>());
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        Message<?> result = interceptor.preSend(message, mockChannel);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertTrue(resultAccessor.getUser() instanceof UsernamePasswordAuthenticationToken);
        return (UsernamePasswordAuthenticationToken) resultAccessor.getUser();
    }

    @Test
    @DisplayName("1. Unauthenticated WebSocket connection rejected with AccessDeniedException")
    void unauthenticatedConnection_ThrowsAccessDeniedException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setSessionAttributes(new HashMap<>());
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(message, mockChannel));
        assertTrue(ex.getMessage().contains("Missing or invalid JWT token"));
    }

    @Test
    @DisplayName("2. Authenticated customer can subscribe to own delivery topic (/topic/deliveries/501/tracking)")
    void customer_CanSubscribeToOwnDelivery_Success() {
        UsernamePasswordAuthenticationToken auth = authenticateConnect(customerAToken);

        StompHeaderAccessor subAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subAccessor.setLeaveMutable(true);
        subAccessor.setDestination("/topic/deliveries/501/tracking");
        subAccessor.setUser(auth);
        Message<byte[]> subMessage = MessageBuilder.createMessage(new byte[0], subAccessor.getMessageHeaders());

        // ACT & ASSERT: Must not throw exception
        Message<?> result = interceptor.preSend(subMessage, mockChannel);
        assertNotNull(result);
    }

    @Test
    @DisplayName("3. Customer cannot subscribe to another customer's delivery topic (/topic/deliveries/502/tracking)")
    void customer_CannotSubscribeToOtherCustomerDelivery_ThrowsAccessDeniedException() {
        UsernamePasswordAuthenticationToken auth = authenticateConnect(customerAToken);

        StompHeaderAccessor subAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subAccessor.setLeaveMutable(true);
        subAccessor.setDestination("/topic/deliveries/502/tracking");
        subAccessor.setUser(auth);
        Message<byte[]> subMessage = MessageBuilder.createMessage(new byte[0], subAccessor.getMessageHeaders());

        // ACT & ASSERT (IDOR Prevention)
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(subMessage, mockChannel));
        assertTrue(ex.getMessage().contains("Access denied to delivery #502"));
    }

    @Test
    @DisplayName("4. Distributor can subscribe to own distributor delivery topic (/topic/deliveries/501/tracking)")
    void distributor_CanSubscribeToOwnDistributorDelivery_Success() {
        UsernamePasswordAuthenticationToken auth = authenticateConnect(distributor1Token);

        StompHeaderAccessor subAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subAccessor.setLeaveMutable(true);
        subAccessor.setDestination("/topic/deliveries/501/tracking");
        subAccessor.setUser(auth);
        Message<byte[]> subMessage = MessageBuilder.createMessage(new byte[0], subAccessor.getMessageHeaders());

        // ACT & ASSERT
        Message<?> result = interceptor.preSend(subMessage, mockChannel);
        assertNotNull(result);
    }

    @Test
    @DisplayName("5. Distributor cannot subscribe to another distributor's delivery topic (/topic/deliveries/502/tracking)")
    void distributor_CannotSubscribeToOtherDistributorDelivery_ThrowsAccessDeniedException() {
        UsernamePasswordAuthenticationToken auth = authenticateConnect(distributor1Token);

        StompHeaderAccessor subAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subAccessor.setLeaveMutable(true);
        subAccessor.setDestination("/topic/deliveries/502/tracking");
        subAccessor.setUser(auth);
        Message<byte[]> subMessage = MessageBuilder.createMessage(new byte[0], subAccessor.getMessageHeaders());

        // ACT & ASSERT
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(subMessage, mockChannel));
        assertTrue(ex.getMessage().contains("Access denied to delivery #502"));
    }

    @Test
    @DisplayName("6. Admin can subscribe to authorized tracking destinations")
    void admin_CanSubscribeToAnyDeliveryTracking_Success() {
        UsernamePasswordAuthenticationToken auth = authenticateConnect(adminToken);

        StompHeaderAccessor subAccessor1 = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subAccessor1.setLeaveMutable(true);
        subAccessor1.setDestination("/topic/deliveries/501/tracking");
        subAccessor1.setUser(auth);
        assertNotNull(interceptor.preSend(MessageBuilder.createMessage(new byte[0], subAccessor1.getMessageHeaders()), mockChannel));

        StompHeaderAccessor subAccessor2 = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subAccessor2.setLeaveMutable(true);
        subAccessor2.setDestination("/topic/deliveries/502/tracking");
        subAccessor2.setUser(auth);
        assertNotNull(interceptor.preSend(MessageBuilder.createMessage(new byte[0], subAccessor2.getMessageHeaders()), mockChannel));
    }

    @Test
    @DisplayName("7. Client cannot bypass authorization by changing delivery ID to nonexistent delivery")
    void client_CannotSubscribeToNonexistentDelivery_ThrowsResourceNotFound() {
        UsernamePasswordAuthenticationToken auth = authenticateConnect(customerAToken);

        StompHeaderAccessor subAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subAccessor.setLeaveMutable(true);
        subAccessor.setDestination("/topic/deliveries/999/tracking");
        subAccessor.setUser(auth);
        Message<byte[]> subMessage = MessageBuilder.createMessage(new byte[0], subAccessor.getMessageHeaders());

        assertThrows(ResourceNotFoundException.class,
                () -> interceptor.preSend(subMessage, mockChannel));
    }

    @Test
    @DisplayName("8. Client-supplied user ID in STOMP headers cannot override authenticated identity")
    void clientSuppliedUserId_CannotOverrideAuthenticatedUser() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.addNativeHeader("Authorization", "Bearer " + customerAToken);
        accessor.addNativeHeader("userId", "100"); // Attempt spoofing admin ID
        accessor.addNativeHeader("user-id", "100");
        accessor.setSessionAttributes(new HashMap<>());
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, mockChannel);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) resultAccessor.getUser();
        assertNotNull(auth);
        assertEquals("customera@gmail.com", auth.getName());
        assertEquals(1L, resultAccessor.getSessionAttributes().get("userId"));
    }

    @Test
    @DisplayName("9. Client-supplied role in STOMP headers cannot override authenticated role")
    void clientSuppliedRole_CannotOverrideAuthenticatedRole() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.addNativeHeader("Authorization", "Bearer " + customerAToken);
        accessor.addNativeHeader("role", "ADMIN"); // Attempt role spoofing
        accessor.addNativeHeader("Role", "ROLE_ADMIN");
        accessor.setSessionAttributes(new HashMap<>());
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, mockChannel);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) resultAccessor.getUser();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority())));
        assertTrue(auth.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
    }

    @Test
    @DisplayName("10. Subscription to arbitrary /topic/** destination is strictly forbidden")
    void client_CannotSubscribeToArbitraryDestination_ThrowsAccessDeniedException() {
        UsernamePasswordAuthenticationToken auth = authenticateConnect(adminToken);

        StompHeaderAccessor subAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subAccessor.setLeaveMutable(true);
        subAccessor.setDestination("/topic/all-vehicles");
        subAccessor.setUser(auth);
        Message<byte[]> subMessage = MessageBuilder.createMessage(new byte[0], subAccessor.getMessageHeaders());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(subMessage, mockChannel));
        assertTrue(ex.getMessage().contains("Unauthorized subscription destination"));
    }

    @Test
    @DisplayName("11. Client message sending (SEND) over WebSocket is strictly rejected")
    void client_CannotSendMessagesOverWebSocket_ThrowsAccessDeniedException() {
        StompHeaderAccessor sendAccessor = StompHeaderAccessor.create(StompCommand.SEND);
        sendAccessor.setLeaveMutable(true);
        sendAccessor.setDestination("/app/tracking");
        Message<byte[]> sendMessage = MessageBuilder.createMessage(new byte[0], sendAccessor.getMessageHeaders());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(sendMessage, mockChannel));
        assertTrue(ex.getMessage().contains("Client message sending is not permitted"));
    }
}
