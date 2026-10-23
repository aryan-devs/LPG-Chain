package com.lpgchain.repository;

import com.lpgchain.entity.CylinderInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CylinderInventoryRepository extends JpaRepository<CylinderInventory, Long> {

    List<CylinderInventory> findByDistributorId(Long distributorId);

    Optional<CylinderInventory> findByDistributorIdAndCylinderCategory(Long distributorId, String cylinderCategory);
}
