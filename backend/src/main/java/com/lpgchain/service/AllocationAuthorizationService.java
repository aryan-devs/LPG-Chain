package com.lpgchain.service;

import com.lpgchain.entity.User;
import org.springframework.security.core.Authentication;

public interface AllocationAuthorizationService {

    User getAuthenticatedUser();

    Authentication getAuthentication();

    boolean isAdmin(Authentication auth);

    boolean isDistributor(Authentication auth);

    boolean isCustomer(Authentication auth);

    void checkCanManageAllocations();

    void checkCanViewDistributorAllocations(Long distributorId);
}
