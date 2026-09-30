package com.lpgchain.repository;

import com.lpgchain.entity.VehicleLocation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleLocationRepository extends JpaRepository<VehicleLocation, Long> {

    /**
     * Finds the latest recorded GPS location for a given vehicle.
     */
    Optional<VehicleLocation> findFirstByVehicleIdOrderByRecordedAtDesc(Long vehicleId);

    /**
     * Finds location history for a given vehicle with pagination/limit support.
     */
    List<VehicleLocation> findByVehicleIdOrderByRecordedAtDesc(Long vehicleId, Pageable pageable);

    /**
     * Finds all location records for a given vehicle ordered by time descending.
     */
    List<VehicleLocation> findByVehicleIdOrderByRecordedAtDesc(Long vehicleId);

    /**
     * Deletes all location records for a given vehicle.
     */
    void deleteByVehicleId(Long vehicleId);
}
