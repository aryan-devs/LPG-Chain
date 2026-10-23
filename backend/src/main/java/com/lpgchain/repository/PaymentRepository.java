package com.lpgchain.repository;

import com.lpgchain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionReference(String transactionReference);

    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.booking b JOIN FETCH p.customer c ORDER BY p.createdAt DESC")
    List<Payment> findAllWithDetails();

    @Query("SELECT p FROM Payment p JOIN FETCH p.booking b JOIN FETCH p.customer c WHERE p.customer.id = :customerId ORDER BY p.createdAt DESC")
    List<Payment> findByCustomerIdWithDetails(@Param("customerId") Long customerId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.booking b JOIN FETCH p.customer c WHERE b.distributor.id = :distributorId ORDER BY p.createdAt DESC")
    List<Payment> findByDistributorIdWithDetails(@Param("distributorId") Long distributorId);
}
