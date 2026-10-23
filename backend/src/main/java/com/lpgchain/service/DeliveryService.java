package com.lpgchain.service;

import com.lpgchain.dto.DeliveryHandoverResponseDTO;
import com.lpgchain.dto.DeliveryRequestDTO;
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
import com.lpgchain.service.notification.DeliveryNotificationService;
import com.lpgchain.service.notification.NotificationResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.lpgchain.entity.CylinderInventory;
import com.lpgchain.entity.InventoryMovement;
import com.lpgchain.entity.Payment;
import com.lpgchain.repository.CylinderInventoryRepository;
import com.lpgchain.repository.InventoryMovementRepository;
import com.lpgchain.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Transactional(readOnly = true)
public class DeliveryService {

    public static final Set<String> VALID_STATUSES = Set.of(
            "ASSIGNED", "PICKED_UP", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"
    );

    public static final Set<String> ACTIVE_STATUSES = Set.of(
            "ASSIGNED", "PICKED_UP", "OUT_FOR_DELIVERY"
    );

    public static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "ASSIGNED",          Set.of("PICKED_UP", "OUT_FOR_DELIVERY", "CANCELLED"),
            "PICKED_UP",        Set.of("OUT_FOR_DELIVERY"),
            "OUT_FOR_DELIVERY", Set.of("DELIVERED"),
            "DELIVERED",        Set.of(),
            "CANCELLED",        Set.of()
    );

    private final DeliveryRepository deliveryRepository;
    private final BookingRepository bookingRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final DistributorRepository distributorRepository;
    private final EntityMapper entityMapper;
    private final DeliveryAuthorizationService deliveryAuthorizationService;
    private final DeliveryNotificationService deliveryNotificationService;
    private final PasswordEncoder passwordEncoder;
    private final CylinderInventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final PaymentRepository paymentRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public DeliveryService(DeliveryRepository deliveryRepository,
                           BookingRepository bookingRepository,
                           DriverRepository driverRepository,
                           VehicleRepository vehicleRepository,
                           DistributorRepository distributorRepository,
                           EntityMapper entityMapper,
                           DeliveryAuthorizationService deliveryAuthorizationService,
                           DeliveryNotificationService deliveryNotificationService,
                           PasswordEncoder passwordEncoder,
                           @Autowired(required = false) CylinderInventoryRepository inventoryRepository,
                           @Autowired(required = false) InventoryMovementRepository movementRepository,
                           @Autowired(required = false) PaymentRepository paymentRepository,
                           @Autowired(required = false) RealtimeEventPublisher realtimeEventPublisher) {
        this.deliveryRepository = deliveryRepository;
        this.bookingRepository = bookingRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.distributorRepository = distributorRepository;
        this.entityMapper = entityMapper;
        this.deliveryAuthorizationService = deliveryAuthorizationService;
        this.deliveryNotificationService = deliveryNotificationService;
        this.passwordEncoder = passwordEncoder;
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.paymentRepository = paymentRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    public DeliveryService(DeliveryRepository deliveryRepository,
                           BookingRepository bookingRepository,
                           DriverRepository driverRepository,
                           VehicleRepository vehicleRepository,
                           DistributorRepository distributorRepository,
                           EntityMapper entityMapper,
                           DeliveryAuthorizationService deliveryAuthorizationService,
                           DeliveryNotificationService deliveryNotificationService,
                           PasswordEncoder passwordEncoder) {
        this(deliveryRepository, bookingRepository, driverRepository, vehicleRepository,
             distributorRepository, entityMapper, deliveryAuthorizationService,
             deliveryNotificationService, passwordEncoder, null, null, null, null);
    }

    /**
     * Atomically creates and assigns an LPG delivery for a booking.
     * Enforces the 10 business rules and state consistency across Booking, Driver, Vehicle, and Delivery.
     */
    @Transactional
    public DeliveryResponseDTO createDelivery(DeliveryRequestDTO dto) {
        if (dto.getBookingId() == null) {
            throw new IllegalArgumentException("bookingId is required");
        }
        if (dto.getDriverId() == null) {
            throw new IllegalArgumentException("driverId is required");
        }
        if (dto.getVehicleId() == null) {
            throw new IllegalArgumentException("vehicleId is required");
        }

        // Rule 1: Booking must exist (lock row for concurrent safety)
        Booking booking = bookingRepository.findByIdWithLock(dto.getBookingId())
                .or(() -> bookingRepository.findById(dto.getBookingId()))
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + dto.getBookingId()));

        // Cannot create delivery for cancelled or delivered booking
        String bookingStatus = booking.getStatus() != null ? booking.getStatus().toUpperCase() : "";
        if ("CANCELLED".equals(bookingStatus)) {
            throw new IllegalArgumentException("Cannot create delivery for a CANCELLED booking");
        }
        if ("DELIVERED".equals(bookingStatus)) {
            throw new IllegalArgumentException("Cannot create delivery for an already DELIVERED booking");
        }

        // Rule 3: Booking must have a valid distributor
        Distributor distributor = booking.getDistributor();
        if (distributor == null) {
            throw new IllegalArgumentException("Booking #" + booking.getId() + " does not have an assigned distributor");
        }

        // Distributor ID consistency check if provided in DTO
        if (dto.getDistributorId() != null && !Objects.equals(dto.getDistributorId(), distributor.getId())) {
            throw new IllegalArgumentException("Provided distributor ID #" + dto.getDistributorId() +
                    " does not match booking distributor ID #" + distributor.getId());
        }

        // ReBAC authorization check
        deliveryAuthorizationService.checkCanCreateDelivery(distributor.getId());

        // Rule 2: Booking must not already have an active Delivery
        if (deliveryRepository.existsByBookingIdAndStatusIn(booking.getId(), ACTIVE_STATUSES)) {
            Optional<Delivery> existingOpt = deliveryRepository.findByBookingId(booking.getId()).stream()
                    .filter(d -> ACTIVE_STATUSES.contains(d.getStatus()))
                    .findFirst();
            String details = existingOpt.map(d -> " (Delivery #" + d.getId() + " - " + d.getStatus() +
                    (d.getDriver() != null ? ", Driver: " + d.getDriver().getName() : "") +
                    (d.getVehicle() != null ? ", Vehicle: " + (d.getVehicle().getVehicleNumber() != null ? d.getVehicle().getVehicleNumber() : d.getVehicle().getVehicleType()) : "") + ")").orElse("");
            throw new IllegalArgumentException("Booking #" + booking.getId() + " already has an active delivery in progress" + details);
        }

        // Rule 4: Driver must exist
        Driver driver = driverRepository.findById(dto.getDriverId())
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + dto.getDriverId()));

        // Rule 5: Vehicle must exist
        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + dto.getVehicleId()));

        // Rule 6: Driver must belong to the same Distributor
        if (driver.getDistributor() == null || !Objects.equals(driver.getDistributor().getId(), distributor.getId())) {
            throw new IllegalArgumentException("Driver #" + driver.getId() + " does not belong to distributor #" + distributor.getId());
        }

        // Rule 7: Vehicle must belong to the same Distributor
        if (vehicle.getDistributor() == null || !Objects.equals(vehicle.getDistributor().getId(), distributor.getId())) {
            throw new IllegalArgumentException("Vehicle #" + vehicle.getId() + " does not belong to distributor #" + distributor.getId());
        }

        // Rule 8: Driver must be available
        if (!"AVAILABLE".equalsIgnoreCase(driver.getStatus())) {
            throw new IllegalArgumentException("Driver #" + driver.getId() + " is currently " + driver.getStatus() + ", not AVAILABLE");
        }

        // Rule 9: Vehicle must be available
        if (!"AVAILABLE".equalsIgnoreCase(vehicle.getStatus())) {
            throw new IllegalArgumentException("Vehicle #" + vehicle.getId() + " is currently " + vehicle.getStatus() + ", not AVAILABLE");
        }

        // State update: Driver -> ASSIGNED
        driver.setStatus("ASSIGNED");
        driverRepository.save(driver);

        // State update: Vehicle -> ASSIGNED
        vehicle.setStatus("ASSIGNED");
        vehicleRepository.save(vehicle);

        // State update: Booking -> CONFIRMED if it was PENDING
        if ("PENDING".equalsIgnoreCase(booking.getStatus())) {
            booking.setStatus("CONFIRMED");
            bookingRepository.save(booking);
        }

        // Create Delivery entity with ASSIGNED status and assignedAt = now()
        Delivery delivery = new Delivery();
        delivery.setBooking(booking);
        delivery.setDriver(driver);
        delivery.setVehicle(vehicle);
        delivery.setDistributor(distributor);
        delivery.setStatus("ASSIGNED");
        delivery.setAssignedAt(LocalDateTime.now());

        Delivery saved = deliveryRepository.save(delivery);
        DeliveryResponseDTO responseDTO = entityMapper.toDeliveryResponseDTO(saved);

        if (realtimeEventPublisher != null) {
            if (booking.getCustomer() != null) {
                realtimeEventPublisher.publishCustomerEvent(booking.getCustomer().getId(), "DELIVERY_SCHEDULED", responseDTO);
            }
            if (distributor != null) {
                realtimeEventPublisher.publishDistributorEvent(distributor.getId(), "DELIVERY_SCHEDULED", responseDTO);
            }
            realtimeEventPublisher.publishAdminEvent("DELIVERY_SCHEDULED", responseDTO);
        }

        return responseDTO;
    }

    public Optional<DeliveryResponseDTO> getDeliveryById(Long id) {
        Optional<Delivery> opt = deliveryRepository.findByIdWithDetails(id);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        Delivery delivery = opt.get();
        deliveryAuthorizationService.checkCanAccessDelivery(delivery);
        return Optional.of(entityMapper.toDeliveryResponseDTO(delivery));
    }

    public List<DeliveryResponseDTO> getDeliveriesByCustomerId(Long customerId) {
        deliveryAuthorizationService.checkCanAccessCustomerDeliveries(customerId);
        return deliveryRepository.findByCustomerIdWithDetails(customerId).stream()
                .map(entityMapper::toDeliveryResponseDTO)
                .toList();
    }

    public List<DeliveryResponseDTO> getDeliveriesByDistributorId(Long distributorId) {
        deliveryAuthorizationService.checkCanAccessDistributorDeliveries(distributorId);
        return deliveryRepository.findByDistributorIdWithDetails(distributorId).stream()
                .map(entityMapper::toDeliveryResponseDTO)
                .toList();
    }

    public List<DeliveryResponseDTO> getDeliveriesByDriverId(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));
        deliveryAuthorizationService.checkCanAccessDriverDeliveries(driver.getDistributor().getId());
        return deliveryRepository.findByDriverIdWithDetails(driverId).stream()
                .map(entityMapper::toDeliveryResponseDTO)
                .toList();
    }

    public List<DeliveryResponseDTO> getDeliveriesByBookingId(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with ID: " + bookingId));
        Authentication auth = deliveryAuthorizationService.getAuthentication();
        if (!deliveryAuthorizationService.isAdmin(auth)) {
            User user = deliveryAuthorizationService.getAuthenticatedUser();
            if (deliveryAuthorizationService.isCustomer(auth)) {
                if (booking.getCustomer() == null || !Objects.equals(booking.getCustomer().getId(), user.getId())) {
                    throw new AccessDeniedException("Forbidden: Access denied to booking #" + bookingId);
                }
            } else if (deliveryAuthorizationService.isDistributor(auth)) {
                if (booking.getDistributor() == null || !deliveryAuthorizationService.isDistributorMember(booking.getDistributor().getId(), user)) {
                    throw new AccessDeniedException("Forbidden: Access denied to booking #" + bookingId);
                }
            } else {
                throw new AccessDeniedException("Forbidden: Access denied");
            }
        }
        List<Delivery> dels = deliveryRepository.findByBookingIdWithDetails(bookingId);
        if (dels == null || dels.isEmpty()) {
            dels = deliveryRepository.findByBookingId(bookingId);
        }
        return dels.stream()
                .map(entityMapper::toDeliveryResponseDTO)
                .toList();
    }

    public List<DeliveryResponseDTO> getAllDeliveries() {
        Authentication auth = deliveryAuthorizationService.getAuthentication();
        if (!deliveryAuthorizationService.isAdmin(auth)) {
            throw new AccessDeniedException("Forbidden: Only administrators can access all deliveries");
        }
        return deliveryRepository.findAllWithDetails().stream()
                .map(entityMapper::toDeliveryResponseDTO)
                .toList();
    }

    /**
     * Atomically advances delivery status along the state machine:
     * ASSIGNED -> PICKED_UP -> OUT_FOR_DELIVERY -> DELIVERED, or ASSIGNED -> CANCELLED.
     */
    @Transactional
    public DeliveryResponseDTO updateDeliveryStatus(Long deliveryId, String newStatus) {
        Delivery delivery = deliveryRepository.findByIdWithDetails(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found with ID: " + deliveryId));

        deliveryAuthorizationService.checkCanManageDelivery(delivery);

        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }

        String targetStatus = newStatus.trim().toUpperCase();
        if (!VALID_STATUSES.contains(targetStatus)) {
            throw new IllegalArgumentException("Invalid status: " + targetStatus + ". Allowed values: " + VALID_STATUSES);
        }

        String currentStatus = delivery.getStatus();
        Set<String> allowedTransitions = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!allowedTransitions.contains(targetStatus)) {
            throw new IllegalArgumentException("Transition from " + currentStatus + " to " + targetStatus + " is not allowed");
        }

        LocalDateTime now = LocalDateTime.now();
        delivery.setStatus(targetStatus);

        Driver driver = delivery.getDriver();
        Vehicle vehicle = delivery.getVehicle();
        Booking booking = delivery.getBooking();

        switch (targetStatus) {
            case "PICKED_UP":
                delivery.setPickedUpAt(now);
                break;

            case "OUT_FOR_DELIVERY":
                delivery.setOutForDeliveryAt(now);
                if (delivery.getPickedUpAt() == null) {
                    delivery.setPickedUpAt(now);
                }
                if (driver != null) {
                    driver.setStatus("ON_DELIVERY");
                    driverRepository.save(driver);
                }
                if (vehicle != null) {
                    vehicle.setStatus("IN_TRANSIT");
                    vehicleRepository.save(vehicle);
                }
                if (booking != null && ("CONFIRMED".equalsIgnoreCase(booking.getStatus()) || "PENDING".equalsIgnoreCase(booking.getStatus()))) {
                    booking.setStatus("OUT_FOR_DELIVERY");
                    bookingRepository.save(booking);
                }
                break;

            case "CANCELLED":
                delivery.setDeliveredAt(null);
                if (driver != null) {
                    driver.setStatus("AVAILABLE");
                    driverRepository.save(driver);
                }
                if (vehicle != null) {
                    vehicle.setStatus("AVAILABLE");
                    vehicleRepository.save(vehicle);
                }
                if (booking != null && !"CANCELLED".equalsIgnoreCase(booking.getStatus())) {
                    booking.setStatus("CONFIRMED");
                    bookingRepository.save(booking);
                }
                break;

            case "DELIVERED":
                if (delivery.getOtpVerifiedAt() == null) {
                    throw new IllegalStateException("Direct transition to DELIVERED is not permitted. Delivery handover must be completed via OTP verification.");
                }
                delivery.setDeliveredAt(now);
                if (driver != null) {
                    driver.setStatus("AVAILABLE");
                    driverRepository.save(driver);
                }
                if (vehicle != null) {
                    vehicle.setStatus("AVAILABLE");
                    vehicleRepository.save(vehicle);
                }
                if (booking != null) {
                    booking.setStatus("DELIVERED");
                    bookingRepository.save(booking);
                }
                break;

            default:
                break;
        }

        Delivery updated = deliveryRepository.save(delivery);
        DeliveryResponseDTO dto = entityMapper.toDeliveryResponseDTO(updated);

        if (realtimeEventPublisher != null) {
            if (booking != null && booking.getCustomer() != null) {
                realtimeEventPublisher.publishCustomerEvent(booking.getCustomer().getId(), "DELIVERY_STATUS_CHANGED", dto);
            }
            if (delivery.getDistributor() != null) {
                realtimeEventPublisher.publishDistributorEvent(delivery.getDistributor().getId(), "DELIVERY_STATUS_CHANGED", dto);
            }
            realtimeEventPublisher.publishAdminEvent("DELIVERY_STATUS_CHANGED", dto);
        }

        return dto;
    }

    /**
     * Atomically assigns or reassigns a driver to an active delivery.
     */
    @Transactional
    public DeliveryResponseDTO assignDriver(Long deliveryId, Long newDriverId) {
        Delivery delivery = deliveryRepository.findByIdWithDetails(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found with ID: " + deliveryId));

        deliveryAuthorizationService.checkCanManageDelivery(delivery);

        if ("DELIVERED".equalsIgnoreCase(delivery.getStatus())) {
            throw new IllegalArgumentException("Cannot reassign driver on a completed (DELIVERED) delivery");
        }

        Driver newDriver = driverRepository.findById(newDriverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + newDriverId));

        if (!Objects.equals(newDriver.getDistributor().getId(), delivery.getDistributor().getId())) {
            throw new IllegalArgumentException("Driver #" + newDriverId + " does not belong to delivery distributor #" + delivery.getDistributor().getId());
        }

        if (!"AVAILABLE".equalsIgnoreCase(newDriver.getStatus())) {
            throw new IllegalArgumentException("Driver #" + newDriverId + " is currently " + newDriver.getStatus() + ", not AVAILABLE");
        }

        // Release previous driver back to AVAILABLE
        Driver oldDriver = delivery.getDriver();
        if (oldDriver != null) {
            oldDriver.setStatus("AVAILABLE");
            driverRepository.save(oldDriver);
        }

        // Assign new driver status matching delivery state
        if ("OUT_FOR_DELIVERY".equalsIgnoreCase(delivery.getStatus())) {
            newDriver.setStatus("ON_DELIVERY");
        } else {
            newDriver.setStatus("ASSIGNED");
        }
        driverRepository.save(newDriver);

        delivery.setDriver(newDriver);
        Delivery updated = deliveryRepository.save(delivery);
        return entityMapper.toDeliveryResponseDTO(updated);
    }

    /**
     * Atomically assigns or reassigns a vehicle to an active delivery.
     */
    @Transactional
    public DeliveryResponseDTO assignVehicle(Long deliveryId, Long newVehicleId) {
        Delivery delivery = deliveryRepository.findByIdWithDetails(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found with ID: " + deliveryId));

        deliveryAuthorizationService.checkCanManageDelivery(delivery);

        if ("DELIVERED".equalsIgnoreCase(delivery.getStatus())) {
            throw new IllegalArgumentException("Cannot reassign vehicle on a completed (DELIVERED) delivery");
        }

        Vehicle newVehicle = vehicleRepository.findById(newVehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found with ID: " + newVehicleId));

        if (!Objects.equals(newVehicle.getDistributor().getId(), delivery.getDistributor().getId())) {
            throw new IllegalArgumentException("Vehicle #" + newVehicleId + " does not belong to delivery distributor #" + delivery.getDistributor().getId());
        }

        if (!"AVAILABLE".equalsIgnoreCase(newVehicle.getStatus())) {
            throw new IllegalArgumentException("Vehicle #" + newVehicleId + " is currently " + newVehicle.getStatus() + ", not AVAILABLE");
        }

        // Release previous vehicle back to AVAILABLE
        Vehicle oldVehicle = delivery.getVehicle();
        if (oldVehicle != null) {
            oldVehicle.setStatus("AVAILABLE");
            vehicleRepository.save(oldVehicle);
        }

        // Assign new vehicle status matching delivery state
        if ("OUT_FOR_DELIVERY".equalsIgnoreCase(delivery.getStatus())) {
            newVehicle.setStatus("IN_TRANSIT");
        } else {
            newVehicle.setStatus("ASSIGNED");
        }
        vehicleRepository.save(newVehicle);

        delivery.setVehicle(newVehicle);
        Delivery updated = deliveryRepository.save(delivery);
        return entityMapper.toDeliveryResponseDTO(updated);
    }

    /**
     * Initiates doorstep handover for an assigned delivery.
     * Generates a cryptographically secure 6-digit OTP, hashes it using BCrypt,
     * stores expiry and retry ceilings, and dispatches via configured notification provider.
     */
    @Transactional
    public DeliveryHandoverResponseDTO initiateHandover(Long deliveryId) {
        Delivery delivery = deliveryRepository.findByIdWithDetails(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found with ID: " + deliveryId));

        deliveryAuthorizationService.checkCanPerformDriverHandover(delivery);

        String currentStatus = delivery.getStatus();
        if ("DELIVERED".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Delivery #" + deliveryId + " has already been completed");
        }
        if (!"OUT_FOR_DELIVERY".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Cannot initiate handover for delivery #" + deliveryId +
                    " in status " + currentStatus + ". Delivery must be OUT_FOR_DELIVERY.");
        }

        // Generate cryptographically secure 6-digit numeric OTP
        int otpCode = secureRandom.nextInt(900000) + 100000;
        String rawOtp = String.valueOf(otpCode);

        LocalDateTime now = LocalDateTime.now();
        delivery.setOtpHash(passwordEncoder.encode(rawOtp));
        delivery.setOtpExpiresAt(now.plusMinutes(10));
        delivery.setOtpAttempts(0);
        delivery.setOtpMaxAttempts(3);
        delivery.setOtpGeneratedAt(now);

        Delivery saved = deliveryRepository.save(delivery);

        Booking booking = saved.getBooking();
        String customerPhone = (booking != null && booking.getCustomer() != null) ? booking.getCustomer().getPhone() : null;
        String customerName = (booking != null && booking.getCustomer() != null) ? booking.getCustomer().getName() : "Customer";

        NotificationResult notificationResult = deliveryNotificationService.sendHandoverOtp(
                customerPhone, customerName, saved.getId(), rawOtp);

        DeliveryHandoverResponseDTO response = new DeliveryHandoverResponseDTO();
        response.setDeliveryId(saved.getId());
        response.setStatus(saved.getStatus());
        response.setHandoverStage("OTP_DISPATCHED");
        response.setRecipientPhoneMasked(maskPhone(customerPhone));
        response.setOtpExpiresAt(saved.getOtpExpiresAt());
        response.setRemainingAttempts(saved.getOtpMaxAttempts());
        response.setNotificationStatus(notificationResult.status());
        response.setNotificationMessage(notificationResult.message());

        // If running in development test mode without real notification provider, provide explicit test OTP
        if (!deliveryNotificationService.getActiveProvider().isRealProvider()) {
            response.setDevTestOtp(rawOtp);
            response.setDevTestNotice("DEVELOPMENT TEST FLOW ONLY: No real SMS notification provider is configured. Do not use in production.");
        }

        return response;
    }

    /**
     * Verifies the customer OTP submitted by the driver.
     * Enforces expiry, retry counters, and transitions delivery to DELIVERED atomically.
     */
    @Transactional
    public DeliveryResponseDTO verifyOtpAndCompleteDelivery(Long deliveryId, String submittedOtp) {
        Delivery delivery = deliveryRepository.findByIdWithDetails(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found with ID: " + deliveryId));

        deliveryAuthorizationService.checkCanPerformDriverHandover(delivery);

        String currentStatus = delivery.getStatus();
        if ("DELIVERED".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Delivery #" + deliveryId + " has already been completed");
        }
        if (!"OUT_FOR_DELIVERY".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Cannot verify OTP for delivery #" + deliveryId +
                    " in status " + currentStatus + ". Delivery must be OUT_FOR_DELIVERY.");
        }

        if (delivery.getOtpHash() == null) {
            throw new IllegalStateException("Delivery handover has not been initiated. Please request an OTP first.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (delivery.getOtpExpiresAt() == null || now.isAfter(delivery.getOtpExpiresAt())) {
            throw new IllegalArgumentException("Delivery OTP has expired. Please initiate a new handover OTP.");
        }

        int attempts = delivery.getOtpAttempts() != null ? delivery.getOtpAttempts() : 0;
        int maxAttempts = delivery.getOtpMaxAttempts() != null ? delivery.getOtpMaxAttempts() : 3;
        if (attempts >= maxAttempts) {
            throw new IllegalArgumentException("Maximum OTP verification attempts exceeded (" + maxAttempts + "). Please initiate a new handover OTP.");
        }

        if (submittedOtp == null || !passwordEncoder.matches(submittedOtp.trim(), delivery.getOtpHash())) {
            int newAttempts = attempts + 1;
            delivery.setOtpAttempts(newAttempts);
            deliveryRepository.save(delivery);
            int remaining = maxAttempts - newAttempts;
            if (remaining <= 0) {
                throw new IllegalArgumentException("Invalid OTP. Maximum attempts reached. Please re-initiate handover.");
            } else {
                throw new IllegalArgumentException("Invalid OTP. " + remaining + " attempt(s) remaining.");
            }
        }

        // OTP verified successfully! Complete the delivery
        delivery.setStatus("DELIVERED");
        delivery.setDeliveredAt(now);
        delivery.setOtpVerifiedAt(now);
        delivery.setOtpHash(null); // Clear OTP hash to prevent replay

        Driver driver = delivery.getDriver();
        if (driver != null) {
            driver.setStatus("AVAILABLE");
            driverRepository.save(driver);
        }

        Vehicle vehicle = delivery.getVehicle();
        if (vehicle != null) {
            vehicle.setStatus("AVAILABLE");
            vehicleRepository.save(vehicle);
        }

        Booking booking = delivery.getBooking();
        if (booking != null) {
            booking.setStatus("DELIVERED");
            bookingRepository.save(booking);
        }

        // Reconcile physical inventory and auditable movement
        if (inventoryRepository != null && booking != null && delivery.getDistributor() != null) {
            String catCode = booking.getCylinderCategory() != null ? booking.getCylinderCategory() : "DOMESTIC_14_2";
            int qty = (booking.getQuantity() != null && booking.getQuantity() > 0) ? booking.getQuantity() : 1;
            Optional<CylinderInventory> invOpt = inventoryRepository.findByDistributorIdAndCylinderCategory(
                    delivery.getDistributor().getId(), catCode);
            if (invOpt.isPresent()) {
                CylinderInventory inv = invOpt.get();
                int currentFilled = inv.getFilledStock() != null ? inv.getFilledStock() : 0;
                int currentEmpty = inv.getEmptyStock() != null ? inv.getEmptyStock() : 0;
                inv.setFilledStock(Math.max(0, currentFilled - qty));
                inv.setEmptyStock(currentEmpty + qty);
                inv.setLastAuditedAt(now);
                inv.setLastAuditedBy(driver != null ? driver.getName() : "Delivery System");
                inventoryRepository.save(inv);

                if (movementRepository != null) {
                    InventoryMovement movement = new InventoryMovement(
                            inv,
                            "DISPATCH",
                            -qty,
                            "Doorstep delivery completed for Booking #" + booking.getId() + " via Delivery #" + delivery.getId(),
                            booking.getBookingNumber() != null ? booking.getBookingNumber() : "DEL-" + delivery.getId(),
                            driver != null ? driver.getName() : "Driver Handover"
                    );
                    movementRepository.save(movement);
                }
            }
        }

        // Reconcile payment for Cash on Delivery / pending payments upon verified doorstep handover
        if (paymentRepository != null && booking != null) {
            Optional<Payment> paymentOpt = paymentRepository.findByBookingId(booking.getId());
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                if (!"PAID".equalsIgnoreCase(payment.getPaymentStatus())) {
                    payment.setPaymentStatus("PAID");
                    payment.setPaidAt(now);
                    payment.setPaymentMethod("CASH_ON_DELIVERY");
                    paymentRepository.save(payment);
                }
            }
            if (!"PAID".equalsIgnoreCase(booking.getPaymentStatus())) {
                booking.setPaymentStatus("PAID");
                bookingRepository.save(booking);
            }
        }

        Delivery saved = deliveryRepository.save(delivery);
        DeliveryResponseDTO dto = entityMapper.toDeliveryResponseDTO(saved);

        if (realtimeEventPublisher != null) {
            if (booking != null && booking.getCustomer() != null) {
                realtimeEventPublisher.publishCustomerEvent(booking.getCustomer().getId(), "DELIVERY_COMPLETED", dto);
            }
            if (delivery.getDistributor() != null) {
                realtimeEventPublisher.publishDistributorEvent(delivery.getDistributor().getId(), "DELIVERY_COMPLETED", dto);
            }
            realtimeEventPublisher.publishAdminEvent("DELIVERY_COMPLETED", dto);
        }

        return dto;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "Registered Contact";
        }
        String clean = phone.trim();
        if (clean.length() <= 4) {
            return "****";
        }
        return clean.substring(0, Math.min(3, clean.length() - 4)) + "****" + clean.substring(clean.length() - 4);
    }
}
