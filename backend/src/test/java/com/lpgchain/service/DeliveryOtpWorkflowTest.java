package com.lpgchain.service;

import com.lpgchain.dto.DeliveryHandoverResponseDTO;
import com.lpgchain.dto.DeliveryResponseDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.Driver;
import com.lpgchain.entity.User;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.DriverRepository;
import com.lpgchain.repository.VehicleRepository;
import com.lpgchain.service.notification.DeliveryNotificationProvider;
import com.lpgchain.service.notification.DeliveryNotificationService;
import com.lpgchain.service.notification.NotificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryOtpWorkflowTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DistributorRepository distributorRepository;

    @Mock
    private DeliveryAuthorizationService deliveryAuthorizationService;

    @Mock
    private DeliveryNotificationService deliveryNotificationService;

    @Mock
    private DeliveryNotificationProvider notificationProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final EntityMapper entityMapper = new EntityMapper();

    private DeliveryService deliveryService;

    private Distributor distributor;
    private User customer;
    private Booking booking;
    private Driver driver;
    private Vehicle vehicle;
    private Delivery delivery;

    @BeforeEach
    void setUp() {
        distributor = new Distributor();
        distributor.setId(1L);
        distributor.setAgencyName("Apex LPG Agency");

        customer = new User(101L, "Rahul Sharma", "rahul@customer.example", "pass", "+919876543210", "Flat 102");

        booking = new Booking(1001L, customer, distributor, "Flat 102", LocalDateTime.now(), 1, "OUT_FOR_DELIVERY");

        driver = new Driver(10L, "Rajesh Driver", "+919123456789", "LIC-987", "ON_DELIVERY", distributor);

        vehicle = new Vehicle(5L, "MH-12-AB-1234", "LPG_TRUCK", 40, "IN_TRANSIT", distributor);

        delivery = new Delivery(501L, booking, driver, vehicle, distributor, "OUT_FOR_DELIVERY",
                LocalDateTime.now().minusHours(1), LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().minusMinutes(15), null);

        deliveryService = new DeliveryService(deliveryRepository, bookingRepository, driverRepository,
                vehicleRepository, distributorRepository, entityMapper, deliveryAuthorizationService,
                deliveryNotificationService, passwordEncoder);
    }

    @Test
    @DisplayName("initiateHandover: successfully generates secure OTP, hashes it, sets 10-min expiry, and triggers notification")
    void initiateHandover_Success() {
        // ARRANGE
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(delivery));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_bcrypt_otp");
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        when(deliveryNotificationService.sendHandoverOtp(eq("+919876543210"), eq("Rahul Sharma"), eq(501L), anyString()))
                .thenReturn(new NotificationResult(true, "DEV_TEST_MODE_ACTIVE", "Simulated dev delivery", "REF-1"));
        when(deliveryNotificationService.getActiveProvider()).thenReturn(notificationProvider);
        when(notificationProvider.isRealProvider()).thenReturn(false);

        // ACT
        DeliveryHandoverResponseDTO response = deliveryService.initiateHandover(501L);

        // ASSERT
        assertNotNull(response);
        assertEquals(501L, response.getDeliveryId());
        assertEquals("OUT_FOR_DELIVERY", response.getStatus());
        assertEquals("OTP_DISPATCHED", response.getHandoverStage());
        assertEquals(3, response.getRemainingAttempts());
        assertNotNull(response.getOtpExpiresAt());
        assertTrue(response.getOtpExpiresAt().isAfter(LocalDateTime.now()));
        assertNotNull(response.getDevTestOtp()); // Present in dev test flow

        ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
        verify(deliveryRepository).save(deliveryCaptor.capture());
        Delivery saved = deliveryCaptor.getValue();

        assertEquals("hashed_bcrypt_otp", saved.getOtpHash());
        assertEquals(0, saved.getOtpAttempts());
        assertEquals(3, saved.getOtpMaxAttempts());
        assertNotNull(saved.getOtpGeneratedAt());
        assertNotNull(saved.getOtpExpiresAt());

        verify(deliveryAuthorizationService).checkCanPerformDriverHandover(delivery);
    }

    @Test
    @DisplayName("initiateHandover: real notification provider does NOT expose dev test OTP")
    void initiateHandover_RealProvider_NoDevOtpExposed() {
        // ARRANGE
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(delivery));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_bcrypt_otp");
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        when(deliveryNotificationService.sendHandoverOtp(any(), any(), any(), any()))
                .thenReturn(new NotificationResult(true, "SMS_SENT", "SMS dispatched via Twilio", "TW-1"));
        when(deliveryNotificationService.getActiveProvider()).thenReturn(notificationProvider);
        when(notificationProvider.isRealProvider()).thenReturn(true);

        // ACT
        DeliveryHandoverResponseDTO response = deliveryService.initiateHandover(501L);

        // ASSERT
        assertNotNull(response);
        assertNull(response.getDevTestOtp()); // Never expose OTP when real provider is configured
        assertNull(response.getDevTestNotice());
        assertEquals("SMS_SENT", response.getNotificationStatus());
    }

    @Test
    @DisplayName("initiateHandover: rejects when delivery is not OUT_FOR_DELIVERY")
    void initiateHandover_RejectsWhenNotOutForDelivery() {
        delivery.setStatus("ASSIGNED");
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(delivery));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> deliveryService.initiateHandover(501L));
        assertTrue(ex.getMessage().contains("Delivery must be OUT_FOR_DELIVERY"));
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("initiateHandover: rejects when delivery is already completed (DELIVERED)")
    void initiateHandover_RejectsWhenAlreadyDelivered() {
        delivery.setStatus("DELIVERED");
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(delivery));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> deliveryService.initiateHandover(501L));
        assertTrue(ex.getMessage().contains("already been completed"));
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("verifyOtpAndCompleteDelivery: succeeds with correct OTP, updates delivery, driver, vehicle, and booking")
    void verifyOtp_Success() {
        // ARRANGE
        delivery.setOtpHash("hashed_otp_value");
        delivery.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        delivery.setOtpAttempts(0);
        delivery.setOtpMaxAttempts(3);

        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(delivery));
        when(passwordEncoder.matches("123456", "hashed_otp_value")).thenReturn(true);
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        DeliveryResponseDTO result = deliveryService.verifyOtpAndCompleteDelivery(501L, "123456");

        // ASSERT
        assertNotNull(result);
        assertEquals("DELIVERED", result.getStatus());
        assertNotNull(result.getDeliveredAt());
        assertNotNull(result.getOtpVerifiedAt());

        // Verify driver & vehicle freed
        assertEquals("AVAILABLE", driver.getStatus());
        verify(driverRepository).save(driver);
        assertEquals("AVAILABLE", vehicle.getStatus());
        verify(vehicleRepository).save(vehicle);

        // Verify booking marked DELIVERED
        assertEquals("DELIVERED", booking.getStatus());
        verify(bookingRepository).save(booking);

        // Verify OTP hash cleared to prevent replay
        assertNull(delivery.getOtpHash());
    }

    @Test
    @DisplayName("verifyOtpAndCompleteDelivery: rejects when handover was not initiated")
    void verifyOtp_RejectsWhenHandoverNotInitiated() {
        delivery.setOtpHash(null);
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(delivery));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> deliveryService.verifyOtpAndCompleteDelivery(501L, "123456"));
        assertTrue(ex.getMessage().contains("Delivery handover has not been initiated"));
    }

    @Test
    @DisplayName("verifyOtpAndCompleteDelivery: rejects when OTP is expired")
    void verifyOtp_RejectsExpiredOtp() {
        delivery.setOtpHash("hashed_otp");
        delivery.setOtpExpiresAt(LocalDateTime.now().minusMinutes(1)); // Expired
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(delivery));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.verifyOtpAndCompleteDelivery(501L, "123456"));
        assertTrue(ex.getMessage().contains("Delivery OTP has expired"));
    }

    @Test
    @DisplayName("verifyOtpAndCompleteDelivery: incorrect OTP increments attempts and reports remaining count")
    void verifyOtp_IncorrectOtp_IncrementsAttempts() {
        delivery.setOtpHash("hashed_otp");
        delivery.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        delivery.setOtpAttempts(0);
        delivery.setOtpMaxAttempts(3);

        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(delivery));
        when(passwordEncoder.matches("999999", "hashed_otp")).thenReturn(false);
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.verifyOtpAndCompleteDelivery(501L, "999999"));
        assertTrue(ex.getMessage().contains("Invalid OTP. 2 attempt(s) remaining."));
        assertEquals(1, delivery.getOtpAttempts());
    }

    @Test
    @DisplayName("verifyOtpAndCompleteDelivery: retry limit reached locks out further attempts")
    void verifyOtp_RetryLimitExceeded_LocksOut() {
        delivery.setOtpHash("hashed_otp");
        delivery.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        delivery.setOtpAttempts(3); // Already at max
        delivery.setOtpMaxAttempts(3);

        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(delivery));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> deliveryService.verifyOtpAndCompleteDelivery(501L, "123456"));
        assertTrue(ex.getMessage().contains("Maximum OTP verification attempts exceeded"));
    }

    @Test
    @DisplayName("verifyOtpAndCompleteDelivery: unauthorized caller is rejected with AccessDeniedException")
    void verifyOtp_UnauthorizedCaller_ThrowsAccessDenied() {
        when(deliveryRepository.findByIdWithDetails(501L)).thenReturn(Optional.of(delivery));
        doThrow(new AccessDeniedException("Forbidden: Customers cannot perform driver delivery handover"))
                .when(deliveryAuthorizationService).checkCanPerformDriverHandover(delivery);

        assertThrows(AccessDeniedException.class,
                () -> deliveryService.verifyOtpAndCompleteDelivery(501L, "123456"));
    }
}
