package com.lpgchain.repository;

import com.lpgchain.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByBookingId(Long bookingId);

    List<Delivery> findByDistributorId(Long distributorId);

    List<Delivery> findByDriverId(Long driverId);

    List<Delivery> findByVehicleId(Long vehicleId);

    List<Delivery> findByVehicleIdAndStatusIn(Long vehicleId, Collection<String> statuses);

    Optional<Delivery> findFirstByVehicleIdAndStatusInOrderByAssignedAtDesc(Long vehicleId, Collection<String> statuses);

    List<Delivery> findByStatus(String status);

    List<Delivery> findByBookingCustomerId(Long customerId);

    boolean existsByBookingIdAndStatusIn(Long bookingId, Collection<String> statuses);

    @Query("SELECT d FROM Delivery d " +
            "JOIN FETCH d.booking b " +
            "JOIN FETCH b.customer " +
            "LEFT JOIN FETCH d.driver " +
            "LEFT JOIN FETCH d.vehicle " +
            "LEFT JOIN FETCH d.distributor " +
            "WHERE d.id = :id")
    Optional<Delivery> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT d FROM Delivery d " +
            "JOIN FETCH d.booking b " +
            "JOIN FETCH b.customer " +
            "LEFT JOIN FETCH d.driver " +
            "LEFT JOIN FETCH d.vehicle " +
            "LEFT JOIN FETCH d.distributor " +
            "WHERE b.customer.id = :customerId ORDER BY d.assignedAt DESC")
    List<Delivery> findByCustomerIdWithDetails(@Param("customerId") Long customerId);

    @Query("SELECT d FROM Delivery d " +
            "JOIN FETCH d.booking b " +
            "JOIN FETCH b.customer " +
            "LEFT JOIN FETCH d.driver " +
            "LEFT JOIN FETCH d.vehicle " +
            "LEFT JOIN FETCH d.distributor " +
            "WHERE d.distributor.id = :distributorId ORDER BY d.assignedAt DESC")
    List<Delivery> findByDistributorIdWithDetails(@Param("distributorId") Long distributorId);

    @Query("SELECT d FROM Delivery d " +
            "JOIN FETCH d.booking b " +
            "JOIN FETCH b.customer " +
            "LEFT JOIN FETCH d.driver " +
            "LEFT JOIN FETCH d.vehicle " +
            "LEFT JOIN FETCH d.distributor " +
            "WHERE d.driver.id = :driverId ORDER BY d.assignedAt DESC")
    List<Delivery> findByDriverIdWithDetails(@Param("driverId") Long driverId);
}
