package com.lpgchain.repository;

import com.lpgchain.entity.CylinderAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CylinderAllocationRepository extends JpaRepository<CylinderAllocation, Long> {

    Optional<CylinderAllocation> findByDistributorIdAndCylinderCategoryAndReportingYearAndReportingMonth(
            Long distributorId, String cylinderCategory, Integer reportingYear, Integer reportingMonth);

    List<CylinderAllocation> findByDistributorIdAndReportingYearAndReportingMonth(
            Long distributorId, Integer reportingYear, Integer reportingMonth);

    List<CylinderAllocation> findByDistributorId(Long distributorId);

    @Query("SELECT DISTINCT a.reportingPeriod FROM CylinderAllocation a ORDER BY a.reportingPeriod DESC")
    List<String> findDistinctReportingPeriods();

    @Query("SELECT a FROM CylinderAllocation a " +
           "JOIN FETCH a.distributor " +
           "WHERE a.reportingYear = :year AND a.reportingMonth = :month")
    List<CylinderAllocation> findByReportingYearAndReportingMonthWithDistributor(
            @Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT a FROM CylinderAllocation a " +
           "JOIN FETCH a.distributor " +
           "WHERE a.distributor.id = :distributorId ORDER BY a.reportingPeriod DESC, a.cylinderCategory ASC")
    List<CylinderAllocation> findByDistributorIdWithDistributorOrderByPeriodDesc(@Param("distributorId") Long distributorId);
}
