package com.lpgchain.service;

import com.lpgchain.dto.DistributorSummaryDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.DistributorRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DistributorService {

    public static final Set<String> VALID_SERVICE_TYPES = Set.of("DOMESTIC", "COMMERCIAL", "BOTH");

    private final DistributorRepository distributorRepository;
    private final EntityMapper entityMapper;

    public DistributorService(DistributorRepository distributorRepository, EntityMapper entityMapper) {
        this.distributorRepository = distributorRepository;
        this.entityMapper = entityMapper;
    }

    public List<DistributorSummaryDTO> getAllDistributors() {
        return distributorRepository.findAll().stream()
                .map(entityMapper::toDistributorSummaryDTO)
                .toList();
    }

    public Optional<DistributorSummaryDTO> getDistributorById(Long id) {
        return distributorRepository.findById(id)
                .map(entityMapper::toDistributorSummaryDTO);
    }

    public Optional<Distributor> getDistributorEntityById(Long id) {
        return distributorRepository.findById(id);
    }

    public List<DistributorSummaryDTO> searchDistributors(String pinCode, String city, String serviceType) {
        if (serviceType != null && !serviceType.trim().isEmpty()) {
            String normalizedServiceType = serviceType.trim().toUpperCase();
            if (!VALID_SERVICE_TYPES.contains(normalizedServiceType)) {
                throw new IllegalArgumentException(
                        "Invalid service type: '" + serviceType + "'. Supported values are DOMESTIC, COMMERCIAL, or BOTH.");
            }
        }

        Specification<Distributor> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (pinCode != null && !pinCode.trim().isEmpty()) {
                predicates.add(cb.equal(cb.trim(root.get("pinCode")), pinCode.trim()));
            }

            if (city != null && !city.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("city")), "%" + city.trim().toLowerCase() + "%"));
            }

            if (serviceType != null && !serviceType.trim().isEmpty()) {
                String upper = serviceType.trim().toUpperCase();
                if ("BOTH".equals(upper)) {
                    predicates.add(cb.equal(root.get("supportedServices"), "BOTH"));
                } else if ("DOMESTIC".equals(upper)) {
                    predicates.add(root.get("supportedServices").in("DOMESTIC", "BOTH"));
                } else if ("COMMERCIAL".equals(upper)) {
                    predicates.add(root.get("supportedServices").in("COMMERCIAL", "BOTH"));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return distributorRepository.findAll(spec).stream()
                .map(entityMapper::toDistributorSummaryDTO)
                .toList();
    }

    @Transactional
    public DistributorSummaryDTO saveDistributor(Distributor distributor) {
        Distributor saved = distributorRepository.save(distributor);
        return entityMapper.toDistributorSummaryDTO(saved);
    }

    @Transactional
    public void deleteDistributor(Long id) {
        distributorRepository.deleteById(id);
    }

    @Transactional
    public DistributorSummaryDTO updateDistributorStatus(Long id, String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be empty");
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("ACTIVE", "SUSPENDED", "PENDING_VERIFICATION").contains(normalized)) {
            throw new IllegalArgumentException("Invalid status: '" + status + "'. Supported values are ACTIVE, SUSPENDED, PENDING_VERIFICATION");
        }

        Distributor distributor = distributorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Distributor not found with ID: " + id));

        distributor.setStatus(normalized);
        Distributor saved = distributorRepository.save(distributor);
        return entityMapper.toDistributorSummaryDTO(saved);
    }
}
