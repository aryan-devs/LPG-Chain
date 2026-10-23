package com.lpgchain.repository;

import com.lpgchain.entity.SavedAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedAddressRepository extends JpaRepository<SavedAddress, Long> {

    List<SavedAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    Optional<SavedAddress> findByIdAndUserId(Long id, Long userId);

    Optional<SavedAddress> findByUserIdAndIsDefaultTrue(Long userId);

    List<SavedAddress> findByUserId(Long userId);
}
