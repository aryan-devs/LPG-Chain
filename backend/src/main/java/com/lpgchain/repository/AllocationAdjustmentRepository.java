package com.lpgchain.repository;

import com.lpgchain.entity.AllocationAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllocationAdjustmentRepository extends JpaRepository<AllocationAdjustment, Long> {

    List<AllocationAdjustment> findByAllocationIdOrderByAdjustedAtDesc(Long allocationId);
}
