package com.lpgchain.repository;

import com.lpgchain.entity.CustomerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerDocumentRepository extends JpaRepository<CustomerDocument, Long> {

    List<CustomerDocument> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<CustomerDocument> findByCustomerIdAndDocumentType(Long customerId, String documentType);

    List<CustomerDocument> findByCustomerIdAndDocumentTypeOrderByCreatedAtDesc(Long customerId, String documentType);

    List<CustomerDocument> findByStatusOrderByCreatedAtAsc(String status);

    @Query("SELECT cd FROM CustomerDocument cd JOIN FETCH cd.customer c ORDER BY cd.createdAt DESC")
    List<CustomerDocument> findAllWithCustomer();

    @Query("SELECT cd FROM CustomerDocument cd JOIN FETCH cd.customer c WHERE cd.status = :status ORDER BY cd.createdAt ASC")
    List<CustomerDocument> findByStatusWithCustomer(@Param("status") String status);

    long countByCustomerIdAndStatus(Long customerId, String status);

    long countByStatus(String status);
}
