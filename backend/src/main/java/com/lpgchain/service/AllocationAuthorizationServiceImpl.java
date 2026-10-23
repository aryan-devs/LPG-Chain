package com.lpgchain.service;

import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.User;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AllocationAuthorizationServiceImpl implements AllocationAuthorizationService {

    private final UserRepository userRepository;
    private final DistributorMembershipRepository distributorMembershipRepository;

    public AllocationAuthorizationServiceImpl(UserRepository userRepository,
                                             DistributorMembershipRepository distributorMembershipRepository) {
        this.userRepository = userRepository;
        this.distributorMembershipRepository = distributorMembershipRepository;
    }

    @Override
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("Forbidden: User is not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Forbidden: Authenticated user not found"));
    }

    @Override
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public boolean isAdmin(Authentication auth) {
        return hasRole(auth, "ADMIN");
    }

    @Override
    public boolean isDistributor(Authentication auth) {
        return hasRole(auth, "DISTRIBUTOR");
    }

    @Override
    public boolean isCustomer(Authentication auth) {
        return hasRole(auth, "CUSTOMER");
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .anyMatch(a -> target.equals(a.getAuthority()));
    }

    @Override
    public void checkCanManageAllocations() {
        Authentication auth = getAuthentication();
        if (!isAdmin(auth)) {
            throw new AccessDeniedException("Forbidden: Only administrators can create or modify official cylinder allocations");
        }
    }

    @Override
    public void checkCanViewDistributorAllocations(Long distributorId) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }

        if (isDistributor(auth)) {
            User user = getAuthenticatedUser();
            Optional<DistributorMembership> membership = distributorMembershipRepository
                    .findByUserIdAndDistributorId(user.getId(), distributorId);
            if (membership.isPresent() && "ACTIVE".equalsIgnoreCase(membership.get().getStatus())) {
                return;
            }
        }

        throw new AccessDeniedException("Forbidden: Access denied to internal allocation details for distributor " + distributorId);
    }
}
