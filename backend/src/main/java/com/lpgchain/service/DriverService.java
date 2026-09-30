package com.lpgchain.service;

import com.lpgchain.dto.DriverRequestDTO;
import com.lpgchain.dto.DriverResponseDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Driver;
import com.lpgchain.entity.User;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.DriverRepository;
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
public class DriverService {

    public static final Set<String> VALID_STATUSES = Set.of(
            "AVAILABLE", "ASSIGNED", "ON_DELIVERY", "INACTIVE"
    );

    private final DriverRepository driverRepository;
    private final DistributorRepository distributorRepository;
    private final DistributorMembershipRepository distributorMembershipRepository;
    private final EntityMapper entityMapper;
    private final DeliveryAuthorizationService deliveryAuthorizationService;

    public DriverService(DriverRepository driverRepository,
                         DistributorRepository distributorRepository,
                         DistributorMembershipRepository distributorMembershipRepository,
                         EntityMapper entityMapper,
                         DeliveryAuthorizationService deliveryAuthorizationService) {
        this.driverRepository = driverRepository;
        this.distributorRepository = distributorRepository;
        this.distributorMembershipRepository = distributorMembershipRepository;
        this.entityMapper = entityMapper;
        this.deliveryAuthorizationService = deliveryAuthorizationService;
    }

    @Transactional
    public DriverResponseDTO createDriver(DriverRequestDTO dto) {
        if (dto.getDistributorId() == null) {
            throw new IllegalArgumentException("Distributor ID is required");
        }

        // ReBAC authorization check
        deliveryAuthorizationService.checkCanManageDistributorResource(dto.getDistributorId());

        Distributor distributor = distributorRepository.findById(dto.getDistributorId())
                .orElseThrow(() -> new IllegalArgumentException("Distributor not found with ID: " + dto.getDistributorId()));

        if (!StringUtils.hasText(dto.getLicenseNumber())) {
            throw new IllegalArgumentException("License number is required");
        }

        if (driverRepository.existsByLicenseNumber(dto.getLicenseNumber().trim())) {
            throw new IllegalArgumentException("Driver with license number " + dto.getLicenseNumber() + " already exists");
        }

        String status = StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim().toUpperCase() : "AVAILABLE";
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid driver status: " + status + ". Allowed: " + VALID_STATUSES);
        }

        Driver driver = new Driver();
        driver.setName(dto.getName().trim());
        driver.setPhone(dto.getPhone().trim());
        driver.setLicenseNumber(dto.getLicenseNumber().trim());
        driver.setStatus(status);
        driver.setDistributor(distributor);

        Driver saved = driverRepository.save(driver);
        return entityMapper.toDriverResponseDTO(saved);
    }

    public Optional<DriverResponseDTO> getDriverById(Long id) {
        Optional<Driver> opt = driverRepository.findById(id);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        Driver driver = opt.get();
        deliveryAuthorizationService.checkCanManageDistributorResource(driver.getDistributor().getId());
        return Optional.of(entityMapper.toDriverResponseDTO(driver));
    }

    public List<DriverResponseDTO> getAllDrivers() {
        Authentication auth = deliveryAuthorizationService.getAuthentication();
        if (deliveryAuthorizationService.isAdmin(auth)) {
            return driverRepository.findAll().stream()
                    .map(entityMapper::toDriverResponseDTO)
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
                .flatMap(distId -> driverRepository.findByDistributorId(distId).stream())
                .map(entityMapper::toDriverResponseDTO)
                .toList();
    }

    public List<DriverResponseDTO> getDriversByDistributorId(Long distributorId) {
        deliveryAuthorizationService.checkCanManageDistributorResource(distributorId);
        return driverRepository.findByDistributorId(distributorId).stream()
                .map(entityMapper::toDriverResponseDTO)
                .toList();
    }

    @Transactional
    public Optional<DriverResponseDTO> updateDriver(Long id, DriverRequestDTO dto) {
        Optional<Driver> opt = driverRepository.findById(id);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        Driver driver = opt.get();
        deliveryAuthorizationService.checkCanManageDistributorResource(driver.getDistributor().getId());

        if (dto.getDistributorId() != null && !dto.getDistributorId().equals(driver.getDistributor().getId())) {
            deliveryAuthorizationService.checkCanManageDistributorResource(dto.getDistributorId());
            Distributor newDistributor = distributorRepository.findById(dto.getDistributorId())
                    .orElseThrow(() -> new IllegalArgumentException("Distributor not found with ID: " + dto.getDistributorId()));
            driver.setDistributor(newDistributor);
        }

        if (StringUtils.hasText(dto.getName())) {
            driver.setName(dto.getName().trim());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            driver.setPhone(dto.getPhone().trim());
        }
        if (StringUtils.hasText(dto.getLicenseNumber())) {
            String newLicense = dto.getLicenseNumber().trim();
            if (driverRepository.existsByLicenseNumberAndIdNot(newLicense, id)) {
                throw new IllegalArgumentException("Driver with license number " + newLicense + " already exists");
            }
            driver.setLicenseNumber(newLicense);
        }
        if (StringUtils.hasText(dto.getStatus())) {
            String newStatus = dto.getStatus().trim().toUpperCase();
            if (!VALID_STATUSES.contains(newStatus)) {
                throw new IllegalArgumentException("Invalid driver status: " + newStatus + ". Allowed: " + VALID_STATUSES);
            }
            driver.setStatus(newStatus);
        }

        Driver updated = driverRepository.save(driver);
        return Optional.of(entityMapper.toDriverResponseDTO(updated));
    }

    @Transactional
    public boolean deleteDriver(Long id) {
        Optional<Driver> opt = driverRepository.findById(id);
        if (opt.isEmpty()) {
            return false;
        }
        Driver driver = opt.get();
        deliveryAuthorizationService.checkCanManageDistributorResource(driver.getDistributor().getId());
        driverRepository.delete(driver);
        return true;
    }
}
