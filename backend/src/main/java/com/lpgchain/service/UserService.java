package com.lpgchain.service;

import com.lpgchain.dto.UserRequestDTO;
import com.lpgchain.dto.UserResponseDTO;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.RoleRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.security.PasswordService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * UserService — User CRUD operations and secure account registration.
 *
 * Password hashing is applied in saveUser() using PasswordService (BCrypt + Pepper).
 * Responses are mapped to UserResponseDTO to prevent exposing entity credentials.
 *
 * Privilege Escalation Safeguards:
 *  - Public registration only provisions CUSTOMER accounts.
 *  - Unauthenticated requests attempting to self-assign ADMIN or DISTRIBUTOR roles
 *    are strictly rejected with AccessDeniedException (403 Forbidden).
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final EntityMapper entityMapper;
    private final EmailPolicyService emailPolicyService;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public UserService(UserRepository userRepository,
                       PasswordService passwordService,
                       EntityMapper entityMapper,
                       EmailPolicyService emailPolicyService,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.entityMapper = entityMapper;
        this.emailPolicyService = emailPolicyService;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapUserWithRole)
                .toList();
    }

    public Optional<UserResponseDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::mapUserWithRole);
    }

    private UserResponseDTO mapUserWithRole(User user) {
        UserResponseDTO dto = entityMapper.toUserResponseDTO(user);
        if (dto != null && userRoleRepository != null) {
            List<UserRole> roles = userRoleRepository.findByUser(user);
            if (roles != null && !roles.isEmpty() && roles.get(0).getRole() != null) {
                dto.setRole(roles.get(0).getRole().getName());
            } else {
                dto.setRole("CUSTOMER");
            }
        }
        return dto;
    }

    /**
     * Creates a new user from a UserRequestDTO with email format validation
     * and privilege escalation prevention.
     */
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("User request cannot be null");
        }

        // Validate email format/policy for customer
        emailPolicyService.validateCustomerEmail(request.getEmail());

        // Check duplicate email
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }

        // Prevent privilege escalation: unauthenticated public registration cannot self-assign ADMIN, DISTRIBUTOR, or other non-customer roles
        if (StringUtils.hasText(request.getRole())) {
            String roleUpper = request.getRole().trim().toUpperCase();
            if (roleUpper.startsWith("ROLE_")) {
                roleUpper = roleUpper.substring(5);
            }
            if (!"CUSTOMER".equals(roleUpper)) {
                throw new AccessDeniedException("Forbidden: Public registration does not permit self-assigning privileged roles (" + roleUpper + ")");
            }
        }

        User user = entityMapper.toUserEntity(request);
        UserResponseDTO saved = saveUser(user);

        // Provision default CUSTOMER role
        if (roleRepository != null && userRoleRepository != null) {
            Role customerRole = roleRepository.findByName("CUSTOMER")
                    .orElseGet(() -> roleRepository.save(new Role("CUSTOMER")));
            userRepository.findById(saved.getId()).ifPresent(persistedUser -> {
                userRoleRepository.save(new UserRole(persistedUser, customerRole));
            });
        }

        saved.setRole("CUSTOMER");
        return saved;
    }

    /**
     * Persists a user with a BCrypt+Pepper hashed password.
     * The raw password supplied in the request body is NEVER stored,
     * and the response is mapped to UserResponseDTO without password.
     */
    @Transactional
    public UserResponseDTO saveUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordService.encode(user.getPassword()));
        }
        User savedUser = userRepository.save(user);
        return entityMapper.toUserResponseDTO(savedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
