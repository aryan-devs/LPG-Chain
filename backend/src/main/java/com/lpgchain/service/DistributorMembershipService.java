package com.lpgchain.service;

import com.lpgchain.dto.DistributorMembershipRequestDTO;
import com.lpgchain.dto.DistributorMembershipResponseDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.User;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * DistributorMembershipService — Manages distributor organization memberships and organizational context.
 *
 * Implements:
 *  - Membership creation & deletion with OWNER / ADMIN authorization checks.
 *  - Membership retrieval for user and distributor with ReBAC verification.
 *  - Validation of active membership status (ACTIVE vs INACTIVE/PENDING).
 *  - Prevention of privilege escalation by MANAGER / STAFF.
 */
@Service
@Transactional(readOnly = true)
public class DistributorMembershipService {

    private static final Set<String> VALID_MEMBERSHIP_ROLES = Set.of("OWNER", "MANAGER", "STAFF");
    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "INACTIVE", "PENDING");

    private final DistributorMembershipRepository distributorMembershipRepository;
    private final DistributorRepository distributorRepository;
    private final UserRepository userRepository;
    private final EntityMapper entityMapper;
    private final EmailPolicyService emailPolicyService;

    public DistributorMembershipService(DistributorMembershipRepository distributorMembershipRepository,
                                      DistributorRepository distributorRepository,
                                      UserRepository userRepository,
                                      EntityMapper entityMapper,
                                      EmailPolicyService emailPolicyService) {
        this.distributorMembershipRepository = distributorMembershipRepository;
        this.distributorRepository = distributorRepository;
        this.userRepository = userRepository;
        this.entityMapper = entityMapper;
        this.emailPolicyService = emailPolicyService;
    }

    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Unauthorized");
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Forbidden: Authenticated user not found"));
    }

    public boolean isAdmin(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public boolean isDistributor(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_DISTRIBUTOR".equals(a.getAuthority()));
    }

    public boolean isOwnerOfDistributor(Long userId, Long distributorId) {
        if (userId == null || distributorId == null) return false;
        return distributorMembershipRepository.findByUserIdAndDistributorId(userId, distributorId)
                .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()))
                .filter(m -> "OWNER".equalsIgnoreCase(m.getMembershipRole()))
                .isPresent();
    }

    /**
     * Creates a new distributor membership.
     * Authorization: Caller must be ADMIN or an active OWNER of the target distributor.
     * Rejects MANAGER, STAFF, CUSTOMER, or unassociated callers with 403 Forbidden.
     */
    @Transactional
    public DistributorMembershipResponseDTO createMembership(DistributorMembershipRequestDTO request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Unauthorized");
        }

        User caller = getAuthenticatedUser();
        Long distributorId = request.getDistributorId();

        // RBAC / ReBAC authorization check
        if (!isAdmin(auth)) {
            if (!isDistributor(auth) || !isOwnerOfDistributor(caller.getId(), distributorId)) {
                throw new AccessDeniedException("Forbidden: Only an OWNER or ADMIN can create memberships for distributor #" + distributorId);
            }
        }

        // Verify target User exists
        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Target user not found with ID: " + request.getUserId()));

        // Enforce Official Email Policy for Distributor
        emailPolicyService.validateDistributorEmail(targetUser.getEmail());

        // Verify target Distributor exists
        Distributor targetDistributor = distributorRepository.findById(distributorId)
                .orElseThrow(() -> new IllegalArgumentException("Target distributor not found with ID: " + distributorId));

        // Normalize and validate membership role
        String rawRole = request.getMembershipRole();
        if (!StringUtils.hasText(rawRole)) {
            throw new IllegalArgumentException("Membership role is required");
        }
        String normalizedRole = rawRole.trim().toUpperCase();
        if (!VALID_MEMBERSHIP_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("Invalid membership role: " + rawRole + ". Must be OWNER, MANAGER, or STAFF.");
        }

        // Normalize and validate status
        String rawStatus = request.getStatus();
        String normalizedStatus = StringUtils.hasText(rawStatus) ? rawStatus.trim().toUpperCase() : "ACTIVE";
        if (!VALID_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid membership status: " + rawStatus + ". Must be ACTIVE, INACTIVE, or PENDING.");
        }

        // Prevent duplicate membership
        Optional<DistributorMembership> existing = distributorMembershipRepository.findByUserIdAndDistributorId(targetUser.getId(), distributorId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("User #" + targetUser.getId() + " already has a membership with distributor #" + distributorId);
        }

        DistributorMembership membership = new DistributorMembership(targetUser, targetDistributor, normalizedRole, normalizedStatus);
        DistributorMembership saved = distributorMembershipRepository.save(membership);
        return entityMapper.toDistributorMembershipResponseDTO(saved);
    }

    /**
     * Retrieves a membership by ID with authorization checks.
     */
    public Optional<DistributorMembershipResponseDTO> getMembershipById(Long id) {
        Optional<DistributorMembership> optional = distributorMembershipRepository.findById(id);
        if (optional.isEmpty()) {
            return Optional.empty();
        }

        DistributorMembership membership = optional.get();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            if (!isAdmin(auth)) {
                User caller = getAuthenticatedUser();
                boolean isSelf = Objects.equals(caller.getId(), membership.getUser().getId());
                boolean isDistributorMember = membership.getDistributor() != null &&
                        distributorMembershipRepository.findByUserIdAndDistributorId(caller.getId(), membership.getDistributor().getId())
                                .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()))
                                .isPresent();

                if (!isSelf && !isDistributorMember) {
                    throw new AccessDeniedException("Forbidden: Access denied to membership #" + id);
                }
            }
        }

        return Optional.of(entityMapper.toDistributorMembershipResponseDTO(membership));
    }

    /**
     * Retrieves memberships for the currently authenticated user.
     */
    public List<DistributorMembershipResponseDTO> getCurrentUserMemberships() {
        User caller = getAuthenticatedUser();
        return distributorMembershipRepository.findByUserId(caller.getId()).stream()
                .map(entityMapper::toDistributorMembershipResponseDTO)
                .toList();
    }

    /**
     * Retrieves memberships for a specific distributor.
     * Authorization: Caller must be ADMIN or an active member of that distributor.
     */
    public List<DistributorMembershipResponseDTO> getMembershipsForDistributor(Long distributorId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            User caller = getAuthenticatedUser();
            boolean isMember = distributorMembershipRepository.findByUserIdAndDistributorId(caller.getId(), distributorId)
                    .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()))
                    .isPresent();
            if (!isMember) {
                throw new AccessDeniedException("Forbidden: Access denied to distributor #" + distributorId + " memberships");
            }
        }

        return distributorMembershipRepository.findByDistributorId(distributorId).stream()
                .map(entityMapper::toDistributorMembershipResponseDTO)
                .toList();
    }

    /**
     * Deletes a membership.
     * Authorization: Caller must be ADMIN or an active OWNER of the distributor.
     */
    @Transactional
    public void deleteMembership(Long id) {
        DistributorMembership membership = distributorMembershipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Membership not found with ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            User caller = getAuthenticatedUser();
            Long distributorId = membership.getDistributor() != null ? membership.getDistributor().getId() : null;
            if (!isOwnerOfDistributor(caller.getId(), distributorId)) {
                throw new AccessDeniedException("Forbidden: Only an OWNER or ADMIN can delete memberships");
            }
        }

        distributorMembershipRepository.deleteById(id);
    }

    /**
     * Validates whether a user has an active membership with the distributor.
     */
    public boolean validateUserBelongsToDistributor(Long userId, Long distributorId) {
        if (userId == null || distributorId == null) return false;
        return distributorMembershipRepository.findByUserIdAndDistributorId(userId, distributorId)
                .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()))
                .isPresent();
    }

    /**
     * Gets the active distributor membership for a given user and distributor ID.
     */
    public Optional<DistributorMembership> getActiveDistributorMembership(Long userId, Long distributorId) {
        if (userId == null || distributorId == null) return Optional.empty();
        return distributorMembershipRepository.findByUserIdAndDistributorId(userId, distributorId)
                .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()));
    }

    /**
     * Returns all active distributor memberships for a user.
     */
    public List<DistributorMembership> getActiveMembershipsForUser(Long userId) {
        if (userId == null) return List.of();
        return distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(userId, "ACTIVE");
    }

    // =========================================================================
    // Legacy / Backward Compatibility Methods
    // =========================================================================

    public List<DistributorMembership> getAllMemberships() {
        return distributorMembershipRepository.findAll();
    }

    @Transactional
    public DistributorMembership saveMembership(DistributorMembership membership) {
        return distributorMembershipRepository.save(membership);
    }
}
