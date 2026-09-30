package com.lpgchain.service;

import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.User;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * DeliveryAuthorizationServiceImpl — Enforces multi-tenant resource-level authorization (ReBAC)
 * and role-based access control (RBAC) for Driver, Vehicle, and Delivery operations.
 */
@Service
@Transactional(readOnly = true)
public class DeliveryAuthorizationServiceImpl implements DeliveryAuthorizationService {

    private final UserRepository userRepository;
    private final DistributorMembershipRepository distributorMembershipRepository;

    public DeliveryAuthorizationServiceImpl(UserRepository userRepository,
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
    public boolean hasRole(Authentication auth, String role) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .anyMatch(a -> target.equals(a.getAuthority()));
    }

    @Override
    public boolean isAdmin(Authentication auth) {
        return hasRole(auth, "ROLE_ADMIN");
    }

    @Override
    public boolean isDistributor(Authentication auth) {
        return hasRole(auth, "ROLE_DISTRIBUTOR");
    }

    @Override
    public boolean isCustomer(Authentication auth) {
        return hasRole(auth, "ROLE_CUSTOMER");
    }

    @Override
    public boolean isDistributorMember(Long distributorId, User user) {
        if (distributorId == null || user == null || user.getId() == null) {
            return false;
        }

        List<DistributorMembership> memberships = distributorMembershipRepository.findByUserId(user.getId());
        if (memberships != null && !memberships.isEmpty()) {
            boolean hasMatchForDistributor = memberships.stream()
                    .anyMatch(m -> m.getDistributor() != null && distributorId.equals(m.getDistributor().getId()));
            if (hasMatchForDistributor) {
                return memberships.stream()
                        .filter(m -> m.getDistributor() != null && distributorId.equals(m.getDistributor().getId()))
                        .anyMatch(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()));
            }
        }

        return distributorMembershipRepository.findByUserIdAndDistributorId(user.getId(), distributorId)
                .map(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()))
                .orElseGet(() -> distributorMembershipRepository.existsByUserIdAndDistributorIdAndStatusIgnoreCase(user.getId(), distributorId, "ACTIVE")
                        || distributorMembershipRepository.existsByUserIdAndDistributorId(user.getId(), distributorId));
    }

    @Override
    public void checkCanAccessDelivery(Delivery delivery) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }
        User user = getAuthenticatedUser();

        if (isCustomer(auth)) {
            if (delivery.getBooking() == null || delivery.getBooking().getCustomer() == null
                    || !Objects.equals(delivery.getBooking().getCustomer().getId(), user.getId())) {
                throw new AccessDeniedException("Forbidden: Access denied to delivery #" + delivery.getId());
            }
            return;
        }

        if (isDistributor(auth)) {
            if (delivery.getDistributor() == null || !isDistributorMember(delivery.getDistributor().getId(), user)) {
                throw new AccessDeniedException("Forbidden: Access denied to delivery #" + delivery.getId());
            }
            return;
        }

        throw new AccessDeniedException("Forbidden: Access denied");
    }

    @Override
    public void checkCanAccessCustomerDeliveries(Long customerId) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }
        if (!isCustomer(auth)) {
            throw new AccessDeniedException("Forbidden: Access denied to customer deliveries");
        }
        User user = getAuthenticatedUser();
        if (!Objects.equals(user.getId(), customerId)) {
            throw new AccessDeniedException("Forbidden: Cannot view another customer's deliveries");
        }
    }

    @Override
    public void checkCanAccessDistributorDeliveries(Long distributorId) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }
        if (!isDistributor(auth)) {
            throw new AccessDeniedException("Forbidden: Access denied to distributor deliveries");
        }
        User user = getAuthenticatedUser();
        if (!isDistributorMember(distributorId, user)) {
            throw new AccessDeniedException("Forbidden: User is not associated with distributor #" + distributorId);
        }
    }

    @Override
    public void checkCanAccessDriverDeliveries(Long driverDistributorId) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }
        if (!isDistributor(auth)) {
            throw new AccessDeniedException("Forbidden: Access denied to driver deliveries");
        }
        User user = getAuthenticatedUser();
        if (!isDistributorMember(driverDistributorId, user)) {
            throw new AccessDeniedException("Forbidden: User is not associated with driver's distributor #" + driverDistributorId);
        }
    }

    @Override
    public void checkCanManageDistributorResource(Long distributorId) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }
        if (isCustomer(auth)) {
            throw new AccessDeniedException("Forbidden: Customers cannot manage drivers or vehicles");
        }
        if (!isDistributor(auth)) {
            throw new AccessDeniedException("Forbidden: Access denied");
        }
        User user = getAuthenticatedUser();
        if (!isDistributorMember(distributorId, user)) {
            throw new AccessDeniedException("Forbidden: User is not associated with distributor #" + distributorId);
        }
    }

    @Override
    public void checkCanCreateDelivery(Long distributorId) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }
        if (isCustomer(auth)) {
            throw new AccessDeniedException("Forbidden: Customers cannot create deliveries");
        }
        if (!isDistributor(auth)) {
            throw new AccessDeniedException("Forbidden: Access denied");
        }
        User user = getAuthenticatedUser();
        if (!isDistributorMember(distributorId, user)) {
            throw new AccessDeniedException("Forbidden: User is not associated with distributor #" + distributorId);
        }
    }

    @Override
    public void checkCanManageDelivery(Delivery delivery) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }
        if (isCustomer(auth)) {
            throw new AccessDeniedException("Forbidden: Customers cannot modify delivery status or assignment");
        }
        if (!isDistributor(auth)) {
            throw new AccessDeniedException("Forbidden: Access denied");
        }
        User user = getAuthenticatedUser();
        if (delivery.getDistributor() == null || !isDistributorMember(delivery.getDistributor().getId(), user)) {
            throw new AccessDeniedException("Forbidden: Access denied to distributor delivery #" + delivery.getId());
        }
    }

    @Override
    public void checkCanAccessVehicleTracking(Vehicle vehicle) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }
        if (isCustomer(auth)) {
            throw new AccessDeniedException("Forbidden: Customers cannot access vehicle tracking directly");
        }
        if (!isDistributor(auth)) {
            throw new AccessDeniedException("Forbidden: Access denied");
        }
        User user = getAuthenticatedUser();
        if (vehicle.getDistributor() == null || !isDistributorMember(vehicle.getDistributor().getId(), user)) {
            throw new AccessDeniedException("Forbidden: Access denied to vehicle #" + vehicle.getId());
        }
    }

    @Override
    public void checkCanIngestVehicleLocation(Vehicle vehicle) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }
        if (isCustomer(auth)) {
            throw new AccessDeniedException("Forbidden: Customers cannot submit vehicle locations");
        }
        if (!isDistributor(auth)) {
            throw new AccessDeniedException("Forbidden: Access denied");
        }
        User user = getAuthenticatedUser();
        if (vehicle.getDistributor() == null || !isDistributorMember(vehicle.getDistributor().getId(), user)) {
            throw new AccessDeniedException("Forbidden: Access denied to ingest location for vehicle #" + vehicle.getId());
        }
    }
}
