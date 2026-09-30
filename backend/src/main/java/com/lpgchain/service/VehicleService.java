package com.lpgchain.service;

import com.lpgchain.dto.VehicleRequestDTO;
import com.lpgchain.dto.VehicleResponseDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.User;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.VehicleRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class VehicleService {

    public static final Set<String> VALID_STATUSES = Set.of(
            "AVAILABLE", "ASSIGNED", "IN_TRANSIT", "INACTIVE"
    );

    private final VehicleRepository vehicleRepository;
    private final DistributorRepository distributorRepository;
    private final DistributorMembershipRepository distributorMembershipRepository;
    private final EntityMapper entityMapper;
    private final DeliveryAuthorizationService deliveryAuthorizationService;

    public VehicleService(VehicleRepository vehicleRepository,
                          DistributorRepository distributorRepository,
                          DistributorMembershipRepository distributorMembershipRepository,
                          EntityMapper entityMapper,
                          DeliveryAuthorizationService deliveryAuthorizationService) {
        this.vehicleRepository = vehicleRepository;
        this.distributorRepository = distributorRepository;
        this.distributorMembershipRepository = distributorMembershipRepository;
        this.entityMapper = entityMapper;
        this.deliveryAuthorizationService = deliveryAuthorizationService;
    }

    @Transactional
    public VehicleResponseDTO createVehicle(VehicleRequestDTO dto) {
        if (dto.getDistributorId() == null) {
            throw new IllegalArgumentException("Distributor ID is required");
        }

        // ReBAC authorization check
        deliveryAuthorizationService.checkCanManageDistributorResource(dto.getDistributorId());

        Distributor distributor = distributorRepository.findById(dto.getDistributorId())
                .orElseThrow(() -> new IllegalArgumentException("Distributor not found with ID: " + dto.getDistributorId()));

        if (!StringUtils.hasText(dto.getVehicleNumber())) {
            throw new IllegalArgumentException("Vehicle number is required");
        }

        if (vehicleRepository.existsByVehicleNumber(dto.getVehicleNumber().trim())) {
            throw new IllegalArgumentException("Vehicle with number " + dto.getVehicleNumber() + " already exists");
        }

        if (!StringUtils.hasText(dto.getVehicleType())) {
            throw new IllegalArgumentException("Vehicle type is required");
        }

        if (dto.getCapacity() == null || dto.getCapacity() <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }

        String status = StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim().toUpperCase() : "AVAILABLE";
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid vehicle status: " + status + ". Allowed: " + VALID_STATUSES);
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleNumber(dto.getVehicleNumber().trim());
        vehicle.setVehicleType(dto.getVehicleType().trim());
        vehicle.setCapacity(dto.getCapacity());
        vehicle.setStatus(status);
        vehicle.setDistributor(distributor);
        if (StringUtils.hasText(dto.getTraccarDeviceId())) {
            vehicle.setTraccarDeviceId(dto.getTraccarDeviceId().trim());
        }

        Vehicle saved = vehicleRepository.save(vehicle);
        return entityMapper.toVehicleResponseDTO(saved);
    }

    public Optional<VehicleResponseDTO> getVehicleById(Long id) {
        Optional<Vehicle> opt = vehicleRepository.findById(id);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        Vehicle vehicle = opt.get();
        deliveryAuthorizationService.checkCanManageDistributorResource(vehicle.getDistributor().getId());
        return Optional.of(entityMapper.toVehicleResponseDTO(vehicle));
    }

    public List<VehicleResponseDTO> getAllVehicles() {
        Authentication auth = deliveryAuthorizationService.getAuthentication();
        if (deliveryAuthorizationService.isAdmin(auth)) {
            return vehicleRepository.findAll().stream()
                    .map(entityMapper::toVehicleResponseDTO)
                    .toList();
        }

        User user = deliveryAuthorizationService.getAuthenticatedUser();
        List<DistributorMembership> memberships = distributorMembershipRepository.findByUserId(user.getId());
        return memberships.stream()
                .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()))
                .map(DistributorMembership::getDistributor)
                .filter(Objects::nonNull)
                .map(Distributor::getId)
                .filter(Objects::nonNull)
                .distinct()
                .flatMap(distId -> vehicleRepository.findByDistributorId(distId).stream())
                .map(entityMapper::toVehicleResponseDTO)
                .toList();
    }

    public List<VehicleResponseDTO> getVehiclesByDistributorId(Long distributorId) {
        deliveryAuthorizationService.checkCanManageDistributorResource(distributorId);
        return vehicleRepository.findByDistributorId(distributorId).stream()
                .map(entityMapper::toVehicleResponseDTO)
                .toList();
    }

    @Transactional
    public Optional<VehicleResponseDTO> updateVehicle(Long id, VehicleRequestDTO dto) {
        Optional<Vehicle> opt = vehicleRepository.findById(id);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        Vehicle vehicle = opt.get();
        deliveryAuthorizationService.checkCanManageDistributorResource(vehicle.getDistributor().getId());

        if (dto.getDistributorId() != null && !dto.getDistributorId().equals(vehicle.getDistributor().getId())) {
            deliveryAuthorizationService.checkCanManageDistributorResource(dto.getDistributorId());
            Distributor newDistributor = distributorRepository.findById(dto.getDistributorId())
                    .orElseThrow(() -> new IllegalArgumentException("Distributor not found with ID: " + dto.getDistributorId()));
            vehicle.setDistributor(newDistributor);
        }

        if (StringUtils.hasText(dto.getVehicleNumber())) {
            String newNumber = dto.getVehicleNumber().trim();
            if (vehicleRepository.existsByVehicleNumberAndIdNot(newNumber, id)) {
                throw new IllegalArgumentException("Vehicle with number " + newNumber + " already exists");
            }
            vehicle.setVehicleNumber(newNumber);
        }
        if (StringUtils.hasText(dto.getVehicleType())) {
            vehicle.setVehicleType(dto.getVehicleType().trim());
        }
        if (dto.getCapacity() != null) {
            if (dto.getCapacity() <= 0) {
                throw new IllegalArgumentException("Capacity must be greater than 0");
            }
            vehicle.setCapacity(dto.getCapacity());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            String newStatus = dto.getStatus().trim().toUpperCase();
            if (!VALID_STATUSES.contains(newStatus)) {
                throw new IllegalArgumentException("Invalid vehicle status: " + newStatus + ". Allowed: " + VALID_STATUSES);
            }
            vehicle.setStatus(newStatus);
        }
        if (dto.getTraccarDeviceId() != null) {
            vehicle.setTraccarDeviceId(dto.getTraccarDeviceId().trim().isEmpty() ? null : dto.getTraccarDeviceId().trim());
        }

        Vehicle updated = vehicleRepository.save(vehicle);
        return Optional.of(entityMapper.toVehicleResponseDTO(updated));
    }

    @Transactional
    public boolean deleteVehicle(Long id) {
        Optional<Vehicle> opt = vehicleRepository.findById(id);
        if (opt.isEmpty()) {
            return false;
        }
        Vehicle vehicle = opt.get();
        deliveryAuthorizationService.checkCanManageDistributorResource(vehicle.getDistributor().getId());
        vehicleRepository.delete(vehicle);
        return true;
    }
}
