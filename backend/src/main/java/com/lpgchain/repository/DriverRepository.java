package com.lpgchain.repository;

import com.lpgchain.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    List<Driver> findByDistributorId(Long distributorId);

    List<Driver> findByStatus(String status);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    List<Driver> findByDistributorIdAndStatus(Long distributorId, String status);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long id);
}
