package com.lpgchain.repository;

import com.lpgchain.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerId(Long customerId);

    Optional<Booking> findByBookingNumber(String bookingNumber);

    List<Booking> findByDistributorId(Long distributorId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.customer LEFT JOIN FETCH b.distributor ORDER BY b.bookingDate DESC")
    List<Booking> findAllWithDetails();

    @Query("SELECT b FROM Booking b JOIN FETCH b.customer LEFT JOIN FETCH b.distributor WHERE b.customer.id = :customerId ORDER BY b.bookingDate DESC")
    List<Booking> findByCustomerIdWithDetails(@Param("customerId") Long customerId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.customer LEFT JOIN FETCH b.distributor WHERE b.distributor.id = :distributorId ORDER BY b.bookingDate DESC")
    List<Booking> findByDistributorIdWithDetails(@Param("distributorId") Long distributorId);

    @Query("SELECT b FROM Booking b JOIN FETCH b.customer LEFT JOIN FETCH b.distributor WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithLock(@Param("id") Long id);

    List<Booking> findByDistributorIdAndStatus(Long distributorId, String status);
}
