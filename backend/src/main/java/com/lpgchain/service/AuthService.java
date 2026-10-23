package com.lpgchain.service;

import com.lpgchain.dto.DistributorSelectionRequestDTO;
import com.lpgchain.dto.DistributorSelectionResponseDTO;
import com.lpgchain.dto.LoginRequestDTO;
import com.lpgchain.dto.LoginResponseDTO;
import com.lpgchain.dto.RoleSelectionRequestDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.exception.InvalidCredentialsException;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.security.JwtTokenProvider;
import com.lpgchain.security.PasswordService;
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
 * AuthService — Orchestrates user authentication, multi-role detection,
 * active-role selection, distributor organization selection, and official email policies.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final DistributorMembershipRepository distributorMembershipRepository;
    private final DistributorRepository distributorRepository;
    private final PasswordService passwordService;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailPolicyService emailPolicyService;

    public AuthService(UserRepository userRepository,
                       UserRoleRepository userRoleRepository,
                       DistributorMembershipRepository distributorMembershipRepository,
                       DistributorRepository distributorRepository,
                       PasswordService passwordService,
                       JwtTokenProvider jwtTokenProvider,
                       EmailPolicyService emailPolicyService) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.distributorMembershipRepository = distributorMembershipRepository;
        this.distributorRepository = distributorRepository;
        this.passwordService = passwordService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailPolicyService = emailPolicyService;
    }

    /**
     * Authenticates user with email and password.
     * If single-role user, immediately issues active-role JWT and resolves distributor context if DISTRIBUTOR.
     * Enforces role-specific email policy on single-role active tokens.
     * If multi-role user, returns roles and requires role selection.
     */
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getPassword() == null || !passwordService.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // Automatic backward-compatible migration: upgrade legacy unpeppered hashes
        if (passwordService.isLegacyUnpeppered(request.getPassword(), user.getPassword())) {
            user.setPassword(passwordService.encode(request.getPassword()));
            userRepository.save(user);
        }

        List<UserRole> userRoles = userRoleRepository.findByUser(user);
        List<String> cleanRoles = userRoles.stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(Role::getName)
                .filter(StringUtils::hasText)
                .map(this::cleanRoleName)
                .distinct()
                .toList();

        if (cleanRoles.size() == 1) {
            String activeRole = cleanRoles.get(0);

            // Enforce Official Email Policy for the single active role
            emailPolicyService.validateEmailForRole(user.getEmail(), activeRole);

            Long activeDistributorId = null;
            boolean distributorSelectionRequired = false;

            if ("DISTRIBUTOR".equalsIgnoreCase(activeRole)) {
                List<DistributorMembership> activeMemberships = distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(user.getId(), "ACTIVE");
                if (activeMemberships.size() == 1) {
                    activeDistributorId = activeMemberships.get(0).getDistributor() != null ? activeMemberships.get(0).getDistributor().getId() : null;
                    distributorSelectionRequired = false;
                } else if (activeMemberships.size() > 1) {
                    activeDistributorId = null;
                    distributorSelectionRequired = true;
                }
            }

            String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), activeRole, activeDistributorId);
            return new LoginResponseDTO(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    cleanRoles,
                    false,
                    activeRole,
                    activeDistributorId,
                    distributorSelectionRequired,
                    token,
                    "Login successful"
            );
        } else if (cleanRoles.size() > 1) {
            // Multi-role user: issue unscoped token for role selection, but NO active-role permissions yet
            String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
            return new LoginResponseDTO(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    cleanRoles,
                    true,
                    null,
                    null,
                    false,
                    token,
                    "Multiple roles available. Select a role."
            );
        } else {
            // User exists in database but has no roles assigned: fail securely
            throw new AccessDeniedException("Forbidden: User account has no assigned roles. Please contact administrator.");
        }
    }

    /**
     * Selects an active role for the authenticated user and generates a new active-role JWT.
     * Enforces role-specific official email policy for the chosen active role.
     */
    @Transactional(readOnly = true)
    public LoginResponseDTO selectRole(RoleSelectionRequestDTO request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Unauthorized");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Forbidden: User not found"));

        if (request == null || !StringUtils.hasText(request.getRole())) {
            throw new AccessDeniedException("Forbidden: Role must be specified");
        }

        String requestedRole = cleanRoleName(request.getRole());

        List<UserRole> userRoles = userRoleRepository.findByUser(user);
        List<String> assignedCleanRoles = userRoles.stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(Role::getName)
                .filter(StringUtils::hasText)
                .map(this::cleanRoleName)
                .distinct()
                .toList();

        boolean hasRole = assignedCleanRoles.stream()
                .anyMatch(r -> r.equalsIgnoreCase(requestedRole));

        if (!hasRole) {
            throw new AccessDeniedException("Forbidden: Role " + requestedRole + " is not assigned to user");
        }

        // Enforce Official Email Policy for the selected active role context
        emailPolicyService.validateEmailForRole(user.getEmail(), requestedRole);

        Long activeDistributorId = null;
        boolean distributorSelectionRequired = false;

        if ("DISTRIBUTOR".equalsIgnoreCase(requestedRole)) {
            List<DistributorMembership> activeMemberships = distributorMembershipRepository.findByUserIdAndStatusIgnoreCase(user.getId(), "ACTIVE");
            if (activeMemberships.size() == 1) {
                activeDistributorId = activeMemberships.get(0).getDistributor() != null ? activeMemberships.get(0).getDistributor().getId() : null;
                distributorSelectionRequired = false;
            } else if (activeMemberships.size() > 1) {
                activeDistributorId = null;
                distributorSelectionRequired = true;
            }
        }

        // Generate a new JWT containing the selected active role and resolved distributorId (if single)
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), requestedRole, activeDistributorId);

        return new LoginResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                assignedCleanRoles,
                false,
                requestedRole,
                activeDistributorId,
                distributorSelectionRequired,
                token,
                "Role selected successfully"
        );
    }

    /**
     * Selects an active distributor organization context for the authenticated user.
     * Verifies that the user has an ACTIVE membership with the requested distributor
     * and an official distributor email.
     */
    @Transactional(readOnly = true)
    public DistributorSelectionResponseDTO selectDistributor(DistributorSelectionRequestDTO request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Unauthorized");
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Forbidden: User not found"));

        // Enforce official distributor email policy
        emailPolicyService.validateDistributorEmail(user.getEmail());

        if (request == null || request.getDistributorId() == null) {
            throw new AccessDeniedException("Forbidden: Distributor ID must be specified");
        }

        Long distributorId = request.getDistributorId();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        String membershipRole = "STAFF";
        String status = "ACTIVE";
        String agencyName = null;

        if (isAdmin) {
            Distributor distributor = distributorRepository.findById(distributorId)
                    .orElseThrow(() -> new AccessDeniedException("Forbidden: Distributor not found with ID: " + distributorId));
            agencyName = distributor.getAgencyName();
            membershipRole = "ADMIN";
        } else {
            // Verify active membership exists for this user and distributor
            Optional<DistributorMembership> membershipOpt = distributorMembershipRepository.findByUserIdAndDistributorId(user.getId(), distributorId)
                    .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()));

            if (membershipOpt.isEmpty()) {
                throw new AccessDeniedException("Forbidden: User does not belong to distributor #" + distributorId);
            }

            DistributorMembership membership = membershipOpt.get();
            agencyName = membership.getDistributor() != null ? membership.getDistributor().getAgencyName() : null;
            membershipRole = membership.getMembershipRole();
            status = membership.getStatus();
        }

        // Issue new scoped JWT embedding role DISTRIBUTOR and the selected distributorId
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), "DISTRIBUTOR", distributorId);

        return new DistributorSelectionResponseDTO(
                user.getId(),
                distributorId,
                agencyName,
                membershipRole,
                status,
                token,
                "Distributor selected successfully"
        );
    }

    public String cleanRoleName(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String trimmed = role.trim().toUpperCase();
        if (trimmed.startsWith("ROLE_")) {
            return trimmed.substring(5);
        }
        return trimmed;
    }
}
