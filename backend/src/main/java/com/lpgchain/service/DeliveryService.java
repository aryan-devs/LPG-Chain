package com.lpgchain.service;

import com.lpgchain.dto.DeliveryRequestDTO;
import com.lpgchain.dto.DeliveryResponseDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.Driver;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.DriverRepository;
import com.lpgchain.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DeliveryService {

    public static final Set<String> VALID_STATUSES = Set.of(
            "ASSIGNED", "PICKED_UP", "OUT_FOR_DELIVERY", "DELIVERED"
    );

    public static final Set<String> ACTIVE_STATUSES = Set.of(
            "ASSIGNED", "PICKED_UP", "OUT_FOR_DELIVERY"
    );

    public static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "ASSIGNED",          Set.of("PICKED_UP"),
            "PICKED_UP",        Set.of("OUT_FOR_DELIVERY"),
            "OUT_FOR_DELIVERY", Set.of("DELIVERED"),
            "DELIVERED",        Set.of()
    );

    private final DeliveryRepository deliveryRepository;
    private final BookingRepository bookingRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final DistributorRepository distributorRepository;
    private final EntityMapper entityMapper;
    private final DeliveryAuthorizationService deliveryAuthorizationService;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           BookingRepository bookingRepository,
                           DriverRepository driverRepository,
                           VehicleRepository vehicleRepository,
                           DistributorRepository distributorRepository,
                           EntityMapper entityMapper,
                           DeliveryAuthorizationService deliveryAuthorizationService) {
        this.deliveryRepository = deliveryRepository;
        this.bookingRepository = bookingRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.distributorRepository = distributorRepository;
        this.entityMapper = entityMapper;
        this.deliveryAuthorizationService = deliveryAuthorizationService;
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

        // Rule 1: Booking must exist
        Booking booking = bookingRepository.findById(dto.getBookingId())
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
            throw new IllegalArgumentException("Booking #" + booking.getId() + " already has an active delivery in progress");
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
        return entityMapper.toDeliveryResponseDTO(saved);
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

    /**
     * Atomically advances delivery status along the state machine:
     * ASSIGNED -> PICKED_UP -> OUT_FOR_DELIVERY -> DELIVERED.
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
                if (driver != null) {
                    driver.setStatus("ON_DELIVERY");
                    driverRepository.save(driver);
                }
                if (vehicle != null) {
                    vehicle.setStatus("IN_TRANSIT");
                    vehicleRepository.save(vehicle);
                }
                if (booking != null && "CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
                    booking.setStatus("OUT_FOR_DELIVERY");
                    bookingRepository.save(booking);
                }
                break;

            case "DELIVERED":
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
        return entityMapper.toDeliveryResponseDTO(updated);
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
}
