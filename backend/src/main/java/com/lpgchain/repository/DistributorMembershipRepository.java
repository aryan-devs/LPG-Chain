package com.lpgchain.repository;

import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistributorMembershipRepository extends JpaRepository<DistributorMembership, Long> {

    List<DistributorMembership> findByUser(User user);

    List<DistributorMembership> findByUserId(Long userId);

    List<DistributorMembership> findByDistributorId(Long distributorId);

    java.util.Optional<DistributorMembership> findByUserIdAndDistributorId(Long userId, Long distributorId);

    List<DistributorMembership> findByUserIdAndStatusIgnoreCase(Long userId, String status);

    List<DistributorMembership> findByDistributorIdAndStatusIgnoreCase(Long distributorId, String status);

    boolean existsByUserIdAndDistributorId(Long userId, Long distributorId);

    boolean existsByUserIdAndDistributorIdAndStatusIgnoreCase(Long userId, Long distributorId, String status);
}
