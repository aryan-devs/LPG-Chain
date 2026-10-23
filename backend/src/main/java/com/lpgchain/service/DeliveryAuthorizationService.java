package com.lpgchain.service;

import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.User;
import com.lpgchain.entity.Vehicle;
import org.springframework.security.core.Authentication;

/**
 * DeliveryAuthorizationService — Interface for multi-tenant resource-level authorization (ReBAC)
 * and role-based access control (RBAC) for Driver, Vehicle, Delivery, and GPS Tracking operations.
 */
public interface DeliveryAuthorizationService {

    User getAuthenticatedUser();

    Authentication getAuthentication();

    boolean hasRole(Authentication auth, String role);

    boolean isAdmin(Authentication auth);

    boolean isDistributor(Authentication auth);

    boolean isCustomer(Authentication auth);

    boolean isDistributorMember(Long distributorId, User user);

    void checkCanAccessDelivery(Delivery delivery);

    void checkCanAccessCustomerDeliveries(Long customerId);

    void checkCanAccessDistributorDeliveries(Long distributorId);

    void checkCanAccessDriverDeliveries(Long driverDistributorId);

    void checkCanManageDistributorResource(Long distributorId);

    void checkCanCreateDelivery(Long distributorId);

    void checkCanManageDelivery(Delivery delivery);

    void checkCanAccessVehicleTracking(Vehicle vehicle);

    void checkCanIngestVehicleLocation(Vehicle vehicle);

    void checkCanPerformDriverHandover(Delivery delivery);
}
