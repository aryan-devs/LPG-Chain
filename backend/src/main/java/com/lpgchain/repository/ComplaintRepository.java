package com.lpgchain.repository;

import com.lpgchain.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Optional<Complaint> findByComplaintNumber(String complaintNumber);

    List<Complaint> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Complaint> findByDistributorIdOrderByCreatedAtDesc(Long distributorId);

    List<Complaint> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT c FROM Complaint c JOIN FETCH c.customer cust LEFT JOIN FETCH c.distributor d LEFT JOIN FETCH c.booking b ORDER BY c.createdAt DESC")
    List<Complaint> findAllWithDetails();

    @Query("SELECT c FROM Complaint c JOIN FETCH c.customer cust LEFT JOIN FETCH c.distributor d LEFT JOIN FETCH c.booking b WHERE c.customer.id = :customerId ORDER BY c.createdAt DESC")
    List<Complaint> findByCustomerIdWithDetails(@Param("customerId") Long customerId);

    @Query("SELECT c FROM Complaint c JOIN FETCH c.customer cust LEFT JOIN FETCH c.distributor d LEFT JOIN FETCH c.booking b WHERE c.distributor.id = :distributorId ORDER BY c.createdAt DESC")
    List<Complaint> findByDistributorIdWithDetails(@Param("distributorId") Long distributorId);

    long countByStatus(String status);
}
