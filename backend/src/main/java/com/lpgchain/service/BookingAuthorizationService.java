package com.lpgchain.service;

import com.lpgchain.dto.BookingRequestDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.User;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * BookingAuthorizationService — Enforces resource-level authorization (ReBAC)
 * and business condition authorization (ABAC) for Booking operations.
 *
 * RBAC answers: "What role does this user have?"
 * ReBAC answers: "Is this user related to this booking?"
 * ABAC answers: "Do the current resource/action/business conditions allow this operation?"
 */
@Service
@Transactional(readOnly = true)
public class BookingAuthorizationService {

    private final UserRepository userRepository;
    private final DistributorMembershipRepository distributorMembershipRepository;

    public BookingAuthorizationService(UserRepository userRepository,
                                     DistributorMembershipRepository distributorMembershipRepository) {
        this.userRepository = userRepository;
        this.distributorMembershipRepository = distributorMembershipRepository;
    }

    /**
     * Resolves the authenticated User entity from the Spring SecurityContextHolder.
     * The JWT subject (email) is the source of truth.
     */
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("Forbidden: User is not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Forbidden: Authenticated user not found"));
    }

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public boolean hasRole(Authentication auth, String role) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .anyMatch(a -> target.equals(a.getAuthority()));
    }

    public boolean isAdmin(Authentication auth) {
        return hasRole(auth, "ROLE_ADMIN");
    }

    public boolean isDistributor(Authentication auth) {
        return hasRole(auth, "ROLE_DISTRIBUTOR");
    }

    public boolean isCustomer(Authentication auth) {
        return hasRole(auth, "ROLE_CUSTOMER");
    }

    // =========================================================================
    // ReBAC: Relationship-Based Access Control Checks
    // =========================================================================

    /**
     * ReBAC: Verifies if the authenticated user owns the customer booking.
     */
    public boolean isCustomerOwner(Booking booking, User user) {
        if (booking == null || booking.getCustomer() == null || user == null) {
            return false;
        }
        return Objects.equals(booking.getCustomer().getId(), user.getId());
    }

    /**
     * ReBAC: Verifies if the booking's distributor is associated with the authenticated user
     * via DistributorMembership.
     */
    public boolean isDistributorAssigned(Booking booking, User user) {
        if (booking == null || booking.getDistributor() == null || user == null) {
            return false;
        }
        Long bookingDistributorId = booking.getDistributor().getId();
        if (bookingDistributorId == null) {
            return false;
        }
        return isDistributorMember(bookingDistributorId, user);
    }

    /**
     * ReBAC + ABAC: Verifies if the user is an active member of the specified distributor organization.
     * Memberships that are INACTIVE or non-active do NOT grant access.
     */
    public boolean isDistributorMember(Long distributorId, User user) {
        if (distributorId == null || user == null || user.getId() == null) {
            return false;
        }

        List<com.lpgchain.entity.DistributorMembership> memberships = distributorMembershipRepository.findByUserId(user.getId());
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

    // =========================================================================
    // ABAC: Attribute-Based Access Control Checks
    // =========================================================================

    /**
     * ABAC: Checks if customer cancellation is permitted in the current booking state.
     * Allowed only when booking is PENDING or CONFIRMED.
     */
    public boolean canCustomerCancel(Booking booking) {
        if (booking == null || booking.getStatus() == null) {
            return false;
        }
        String status = booking.getStatus().toUpperCase();
        return "PENDING".equals(status) || "CONFIRMED".equals(status);
    }

    // =========================================================================
    // High-Level Authorization Enforcement (throws AccessDeniedException on fail)
    // =========================================================================

    /**
     * Authorizes reading a single booking.
     * - Admin: ALLOW
     * - Customer: must own booking
     * - Distributor: must be assigned to booking
     */
    public void checkCanAccessBooking(Booking booking) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }
        User user = getAuthenticatedUser();

        if (isCustomer(auth)) {
            if (!isCustomerOwner(booking, user)) {
                throw new AccessDeniedException("Forbidden: Access denied to booking #" + booking.getId());
            }
            return;
        }

        if (isDistributor(auth)) {
            if (!isDistributorAssigned(booking, user)) {
                throw new AccessDeniedException("Forbidden: Access denied to booking #" + booking.getId());
            }
            return;
        }

        throw new AccessDeniedException("Forbidden: Access denied");
    }

    /**
     * Authorizes querying bookings by customer ID.
     * - Admin: ALLOW
     * - Customer: customerId must match authenticated user's ID
     * - Distributor: DENY (403)
     */
    public void checkCanAccessCustomerBookings(Long customerId) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }
        if (!isCustomer(auth)) {
            throw new AccessDeniedException("Forbidden: Access denied to customer bookings");
        }
        User user = getAuthenticatedUser();
        if (!Objects.equals(user.getId(), customerId)) {
            throw new AccessDeniedException("Forbidden: Cannot view another customer's bookings");
        }
    }

    /**
     * Authorizes querying bookings by distributor ID.
     * - Admin: ALLOW
     * - Distributor: user must be an active member of distributorId
     * - Customer: DENY (403)
     */
    public void checkCanAccessDistributorBookings(Long distributorId) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }
        if (!isDistributor(auth)) {
            throw new AccessDeniedException("Forbidden: Access denied to distributor bookings");
        }
        User user = getAuthenticatedUser();
        if (!isDistributorMember(distributorId, user)) {
            throw new AccessDeniedException("Forbidden: User is not associated with distributor #" + distributorId);
        }
    }

    /**
     * Authorizes creating a booking.
     * - Admin: ALLOW for any customerId
     * - Customer: must create for own customer ID; rejects client tampering
     * - Distributor: DENY (403)
     */
    public void checkCanCreateBooking(BookingRequestDTO dto) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }
        if (!isCustomer(auth)) {
            throw new AccessDeniedException("Forbidden: Only customers or administrators can create bookings");
        }
        User user = getAuthenticatedUser();
        if (dto.getCustomerId() == null) {
            dto.setCustomerId(user.getId());
        } else if (!Objects.equals(dto.getCustomerId(), user.getId())) {
            throw new AccessDeniedException("Forbidden: Cannot create a booking on behalf of another customer");
        }
    }

    /**
     * Authorizes updating a booking's status.
     * - Admin: ALLOW (state transition validation will still enforce 400 for illegal transitions)
     * - Customer:
     *     - Must own the booking (ReBAC, 403 if not)
     *     - Must ONLY attempt CANCELLED (ABAC: delivery status updates yield 403)
     * - Distributor:
     *     - Must be assigned to booking (ReBAC, 403 if not)
     *     - May update delivery status and cancellations following business state machine
     */
    public void checkCanUpdateBookingStatus(Booking booking, String newStatus) {
        Authentication auth = getAuthentication();
        if (isAdmin(auth)) {
            return;
        }
        User user = getAuthenticatedUser();

        if (isCustomer(auth)) {
            // ReBAC check: customer must own the booking
            if (!isCustomerOwner(booking, user)) {
                throw new AccessDeniedException("Forbidden: Access denied to booking #" + booking.getId());
            }

            // ABAC check: customer can ONLY cancel, never update delivery statuses
            if (!"CANCELLED".equalsIgnoreCase(newStatus)) {
                throw new AccessDeniedException("Forbidden: Customers cannot update delivery status");
            }
            return;
        }

        if (isDistributor(auth)) {
            // ReBAC check: distributor must be assigned to the booking
            if (!isDistributorAssigned(booking, user)) {
                throw new AccessDeniedException("Forbidden: Booking is not assigned to your distributor organization");
            }
            return;
        }

        throw new AccessDeniedException("Forbidden: Access denied");
    }

    /**
     * Authorizes assigning a distributor to a booking.
     * - Admin: ALLOW
     * - All others: DENY (403)
     */
    public void checkCanAssignDistributor() {
        Authentication auth = getAuthentication();
        if (!isAdmin(auth)) {
            throw new AccessDeniedException("Forbidden: Only administrators can assign distributors");
        }
    }

    /**
     * Authorizes deleting a booking.
     * - Admin: ALLOW
     * - All others: DENY (403)
     */
    public void checkCanDeleteBooking() {
        Authentication auth = getAuthentication();
        if (!isAdmin(auth)) {
            throw new AccessDeniedException("Forbidden: Only administrators can delete bookings");
        }
    }
}
