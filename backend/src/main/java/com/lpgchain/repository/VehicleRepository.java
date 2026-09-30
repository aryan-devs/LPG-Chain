package com.lpgchain.repository;

import com.lpgchain.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByDistributorId(Long distributorId);

    List<Vehicle> findByStatus(String status);

    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);

    List<Vehicle> findByDistributorIdAndStatus(Long distributorId, String status);

    boolean existsByVehicleNumber(String vehicleNumber);

    boolean existsByVehicleNumberAndIdNot(String vehicleNumber, Long id);

    Optional<Vehicle> findByTraccarDeviceId(String traccarDeviceId);
}
