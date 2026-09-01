package com.lpgchain.repository;

import com.lpgchain.entity.DistributorMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributorMembershipRepository extends JpaRepository<DistributorMembership, Long> {
}
