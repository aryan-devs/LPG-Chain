package com.lpgchain.service;

import com.lpgchain.entity.Distributor;
import com.lpgchain.repository.DistributorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DistributorService {

    private final DistributorRepository distributorRepository;

    public DistributorService(DistributorRepository distributorRepository) {
        this.distributorRepository = distributorRepository;
    }

    public List<Distributor> getAllDistributors() {
        return distributorRepository.findAll();
    }

    public Optional<Distributor> getDistributorById(Long id) {
        return distributorRepository.findById(id);
    }

    public Distributor saveDistributor(Distributor distributor) {
        return distributorRepository.save(distributor);
    }

    public void deleteDistributor(Long id) {
        distributorRepository.deleteById(id);
    }
}
