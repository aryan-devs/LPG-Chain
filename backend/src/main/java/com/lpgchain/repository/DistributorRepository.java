package com.lpgchain.repository;

import com.lpgchain.entity.Distributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DistributorRepository extends JpaRepository<Distributor, Long>, JpaSpecificationExecutor<Distributor> {

    Optional<Distributor> findByLicenceNumber(String licenceNumber);
}
