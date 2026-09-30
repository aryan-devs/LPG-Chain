package com.lpgchain.service;

import com.lpgchain.dto.VehicleLocationEventDTO;
import com.lpgchain.dto.VehicleLocationRequestDTO;
import com.lpgchain.dto.VehicleLocationResponseDTO;
import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.entity.VehicleLocation;
import com.lpgchain.exception.ResourceNotFoundException;
import com.lpgchain.integration.traccar.TraccarClient;
import com.lpgchain.integration.traccar.TraccarPositionDTO;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.VehicleLocationRepository;
import com.lpgchain.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * VehicleTrackingServiceImpl — Production implementation of VehicleTrackingService.
 * Enforces business-level GPS validation, multi-tenant ReBAC authorization, coordinates
 * with the Traccar integration boundary, and broadcasts real-time updates to STOMP topics.
 */
@Service
public class VehicleTrackingServiceImpl implements VehicleTrackingService {

    private static final Logger log = LoggerFactory.getLogger(VehicleTrackingServiceImpl.class);

    private final VehicleRepository vehicleRepository;
    private final VehicleLocationRepository vehicleLocationRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryAuthorizationService deliveryAuthorizationService;
    private final EntityMapper entityMapper;
    private final TraccarClient traccarClient;
    private final VehicleTrackingEventPublisher vehicleTrackingEventPublisher;
    private final VehicleTrackingRedisService vehicleTrackingRedisService;
    private final VehicleTrackingRedisPublisher vehicleTrackingRedisPublisher;

    @Autowired
    public VehicleTrackingServiceImpl(VehicleRepository vehicleRepository,
                                     VehicleLocationRepository vehicleLocationRepository,
                                     DeliveryRepository deliveryRepository,
                                     DeliveryAuthorizationService deliveryAuthorizationService,
                                     EntityMapper entityMapper,
                                     TraccarClient traccarClient,
                                     VehicleTrackingEventPublisher vehicleTrackingEventPublisher,
                                     VehicleTrackingRedisService vehicleTrackingRedisService,
                                     VehicleTrackingRedisPublisher vehicleTrackingRedisPublisher) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleLocationRepository = vehicleLocationRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryAuthorizationService = deliveryAuthorizationService;
        this.entityMapper = entityMapper;
        this.traccarClient = traccarClient;
        this.vehicleTrackingEventPublisher = vehicleTrackingEventPublisher;
        this.vehicleTrackingRedisService = vehicleTrackingRedisService;
        this.vehicleTrackingRedisPublisher = vehicleTrackingRedisPublisher;
    }

    public VehicleTrackingServiceImpl(VehicleRepository vehicleRepository,
                                      VehicleLocationRepository vehicleLocationRepository,
                                      DeliveryRepository deliveryRepository,
                                      DeliveryAuthorizationService deliveryAuthorizationService,
                                      EntityMapper entityMapper,
                                      TraccarClient traccarClient,
                                      VehicleTrackingEventPublisher vehicleTrackingEventPublisher) {
        this(vehicleRepository, vehicleLocationRepository, deliveryRepository, deliveryAuthorizationService,
                entityMapper, traccarClient, vehicleTrackingEventPublisher, null, null);
    }

    @Override
    @Transactional
    public VehicleLocationResponseDTO recordLocation(Long vehicleId, VehicleLocationRequestDTO dto) {
        validateLocationData(dto);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId));

        deliveryAuthorizationService.checkCanIngestVehicleLocation(vehicle);

        VehicleLocation location = new VehicleLocation(
                vehicle,
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getSpeed(),
                dto.getHeading(),
                dto.getRecordedAt()
        );

        VehicleLocation saved = vehicleLocationRepository.save(location);
        log.debug("Recorded location ID {} for vehicle {}", saved.getId(), vehicle.getVehicleNumber());

        publishActiveDeliveryTrackingEvent(vehicle, saved);

        return entityMapper.toVehicleLocationResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VehicleLocationResponseDTO> getLatestLocation(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId));

        deliveryAuthorizationService.checkCanAccessVehicleTracking(vehicle);

        // 1. Try retrieving latest location from fast-changing Redis cache
        if (vehicleTrackingRedisService != null) {
            Optional<VehicleLocationResponseDTO> cached = vehicleTrackingRedisService.getLatestLocation(vehicleId);
            if (cached.isPresent()) {
                return cached;
            }
        }

        // 2. Fallback to durable MySQL history on Redis cache miss or error
        Optional<VehicleLocationResponseDTO> fromDb = vehicleLocationRepository
                .findFirstByVehicleIdOrderByRecordedAtDesc(vehicleId)
                .map(entityMapper::toVehicleLocationResponseDTO);

        // 3. Populate Redis cache on cache miss
        if (fromDb.isPresent() && vehicleTrackingRedisService != null) {
            vehicleTrackingRedisService.saveLatestLocation(fromDb.get());
        }

        return fromDb;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleLocationResponseDTO> getLocationHistory(Long vehicleId, int limit) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId));

        deliveryAuthorizationService.checkCanAccessVehicleTracking(vehicle);

        int effectiveLimit = Math.min(Math.max(limit, 1), 100);
        List<VehicleLocation> records = vehicleLocationRepository.findByVehicleIdOrderByRecordedAtDesc(
                vehicleId, PageRequest.of(0, effectiveLimit));

        return records.stream()
                .map(entityMapper::toVehicleLocationResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VehicleLocationResponseDTO> getLatestLocationByDeliveryId(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found with ID: " + deliveryId));

        deliveryAuthorizationService.checkCanAccessDelivery(delivery);

        Vehicle vehicle = delivery.getVehicle();
        if (vehicle == null) {
            throw new ResourceNotFoundException("No vehicle assigned to delivery #" + deliveryId);
        }

        // 1. Try retrieving latest location from fast-changing Redis cache
        if (vehicleTrackingRedisService != null) {
            Optional<VehicleLocationResponseDTO> cached = vehicleTrackingRedisService.getLatestLocation(vehicle.getId());
            if (cached.isPresent()) {
                return cached;
            }
        }

        // 2. Fallback to durable MySQL history on Redis cache miss or error
        Optional<VehicleLocationResponseDTO> fromDb = vehicleLocationRepository
                .findFirstByVehicleIdOrderByRecordedAtDesc(vehicle.getId())
                .map(entityMapper::toVehicleLocationResponseDTO);

        // 3. Populate Redis cache on cache miss
        if (fromDb.isPresent() && vehicleTrackingRedisService != null) {
            vehicleTrackingRedisService.saveLatestLocation(fromDb.get());
        }

        return fromDb;
    }

    @Override
    @Transactional
    public void setTraccarDeviceId(Long vehicleId, String traccarDeviceId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId));

        deliveryAuthorizationService.checkCanManageDistributorResource(vehicle.getDistributor().getId());

        vehicle.setTraccarDeviceId(StringUtils.hasText(traccarDeviceId) ? traccarDeviceId.trim() : null);
        vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getTraccarDeviceId(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId));

        deliveryAuthorizationService.checkCanAccessVehicleTracking(vehicle);
        return Optional.ofNullable(vehicle.getTraccarDeviceId());
    }

    @Override
    @Transactional
    public Optional<VehicleLocationResponseDTO> syncLocationFromTraccar(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId));

        deliveryAuthorizationService.checkCanAccessVehicleTracking(vehicle);

        if (!StringUtils.hasText(vehicle.getTraccarDeviceId())) {
            log.info("Vehicle {} has no Traccar device mapping configured", vehicle.getVehicleNumber());
            return Optional.empty();
        }

        Optional<TraccarPositionDTO> positionOpt = traccarClient.getLatestPosition(vehicle.getTraccarDeviceId());
        if (positionOpt.isEmpty()) {
            return Optional.empty();
        }

        TraccarPositionDTO pos = positionOpt.get();
        if (pos.getLatitude() == null || pos.getLongitude() == null) {
            return Optional.empty();
        }

        LocalDateTime recordedTime = parseFixTime(pos.getFixTime(), pos.getDeviceTime());

        VehicleLocation location = new VehicleLocation(
                vehicle,
                pos.getLatitude(),
                pos.getLongitude(),
                pos.getSpeed() != null ? pos.getSpeed() : 0.0,
                pos.getCourse() != null ? pos.getCourse() : 0.0,
                recordedTime
        );

        VehicleLocation saved = vehicleLocationRepository.save(location);
        publishActiveDeliveryTrackingEvent(vehicle, saved);
        return Optional.of(entityMapper.toVehicleLocationResponseDTO(saved));
    }

    private void publishActiveDeliveryTrackingEvent(Vehicle vehicle, VehicleLocation saved) {
        if (vehicle == null || vehicle.getId() == null || saved == null) {
            return;
        }

        Runnable cacheAction = () -> {
            if (vehicleTrackingRedisService != null) {
                vehicleTrackingRedisService.saveLatestLocation(vehicle, saved);
            }
        };

        Optional<Delivery> activeDeliveryOpt = deliveryRepository.findFirstByVehicleIdAndStatusInOrderByAssignedAtDesc(
                vehicle.getId(), DeliveryService.ACTIVE_STATUSES);

        if (activeDeliveryOpt.isEmpty()) {
            log.debug("No active delivery found for vehicle {}, skipping tracking broadcast", vehicle.getVehicleNumber());
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        cacheAction.run();
                    }
                });
            } else {
                cacheAction.run();
            }
            return;
        }

        Delivery activeDelivery = activeDeliveryOpt.get();
        Long deliveryId = activeDelivery.getId();

        VehicleLocationEventDTO event = new VehicleLocationEventDTO(
                deliveryId,
                vehicle.getId(),
                vehicle.getVehicleNumber(),
                saved.getLatitude(),
                saved.getLongitude(),
                saved.getSpeed(),
                saved.getHeading(),
                saved.getRecordedAt()
        );

        Runnable dispatchAction = () -> {
            cacheAction.run();

            boolean publishedToRedis = false;
            if (vehicleTrackingRedisPublisher != null) {
                publishedToRedis = vehicleTrackingRedisPublisher.publishTrackingEvent(event);
            }

            // Fallback to local WebSocket broadcast if Redis Pub/Sub is unavailable or not configured
            if (!publishedToRedis) {
                log.debug("Directly broadcasting tracking event to local STOMP destination for delivery #{}", deliveryId);
                vehicleTrackingEventPublisher.publishLocationUpdate(deliveryId, saved);
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchAction.run();
                }
            });
        } else {
            dispatchAction.run();
        }
    }

    private LocalDateTime parseFixTime(String fixTime, String fallbackTime) {
        String timeStr = StringUtils.hasText(fixTime) ? fixTime : fallbackTime;
        if (!StringUtils.hasText(timeStr)) {
            return LocalDateTime.now();
        }
        try {
            return OffsetDateTime.parse(timeStr).toLocalDateTime();
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(timeStr);
            } catch (Exception e) {
                return LocalDateTime.now();
            }
        }
    }

    private void validateLocationData(VehicleLocationRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Location data cannot be null");
        }
        if (dto.getLatitude() == null || dto.getLatitude() < -90.0 || dto.getLatitude() > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90.0 and 90.0");
        }
        if (dto.getLongitude() == null || dto.getLongitude() < -180.0 || dto.getLongitude() > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180.0 and 180.0");
        }
        if (dto.getSpeed() == null || dto.getSpeed() < 0.0) {
            throw new IllegalArgumentException("Speed must not be negative");
        }
        if (dto.getHeading() == null || dto.getHeading() < 0.0 || dto.getHeading() >= 360.0) {
            throw new IllegalArgumentException("Heading must be between 0.0 and less than 360.0 degrees");
        }
        if (dto.getRecordedAt() == null) {
            throw new IllegalArgumentException("recordedAt timestamp is required");
        }
    }
}
