package com.lpgchain.service;

import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.repository.DistributorMembershipRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DistributorMembershipService {

    private final DistributorMembershipRepository distributorMembershipRepository;

    public DistributorMembershipService(DistributorMembershipRepository distributorMembershipRepository) {
        this.distributorMembershipRepository = distributorMembershipRepository;
    }

    public List<DistributorMembership> getAllMemberships() {
        return distributorMembershipRepository.findAll();
    }

    public Optional<DistributorMembership> getMembershipById(Long id) {
        return distributorMembershipRepository.findById(id);
    }

    public DistributorMembership saveMembership(DistributorMembership membership) {
        return distributorMembershipRepository.save(membership);
    }

    public void deleteMembership(Long id) {
        distributorMembershipRepository.deleteById(id);
    }
}
