package com.lpgchain.config;

import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.RoleRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.security.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ProductionDemoAccountProvisioner — Opt-in, idempotent bootstrapper for initial
 * demo Admin and Distributor accounts in production cloud databases (e.g. TiDB Cloud).
 *
 * Activation:
 *  Strictly gated by `lpgchain.provision-demo-accounts=true` (env var `PROVISION_DEMO_ACCOUNTS=true`).
 *  Defaults to `false`. Does not expose any HTTP endpoint.
 *
 * Guarantees:
 *  1. Checks `findByEmail` before inserting. Never overwrites or resets existing users or passwords.
 *  2. Uses application-level {@link PasswordService} to encode credentials with the production pepper.
 *  3. Assigns verified ADMIN and DISTRIBUTOR roles via `user_role` mapping.
 *  4. Associates the Distributor user with an agency and creates an ACTIVE DistributorMembership
 *     required by {@link com.lpgchain.service.AuthService} to resolve distributor scope.
 *  5. Never logs raw passwords, password hashes, or pepper secrets.
 */
@Component
public class ProductionDemoAccountProvisioner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionDemoAccountProvisioner.class);

    private final boolean provisionDemoAccountsEnabled;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final DistributorRepository distributorRepository;
    private final DistributorMembershipRepository distributorMembershipRepository;
    private final PasswordService passwordService;

    public ProductionDemoAccountProvisioner(
            @Value("${lpgchain.provision-demo-accounts:false}") boolean provisionDemoAccountsEnabled,
            RoleRepository roleRepository,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            DistributorRepository distributorRepository,
            DistributorMembershipRepository distributorMembershipRepository,
            PasswordService passwordService) {
        this.provisionDemoAccountsEnabled = provisionDemoAccountsEnabled;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.distributorRepository = distributorRepository;
        this.distributorMembershipRepository = distributorMembershipRepository;
        this.passwordService = passwordService;
    }

    @Override
    public void run(String... args) {
        if (!provisionDemoAccountsEnabled) {
            log.debug("Production demo account provisioning is disabled (PROVISION_DEMO_ACCOUNTS=false). Skipping.");
            return;
        }

        log.info("[PROVISION] Explicit demo account provisioning enabled (PROVISION_DEMO_ACCOUNTS=true). Starting idempotent check...");
        try {
            provisionDemoAccounts();
            log.info("[PROVISION] Demo account provisioning check completed successfully.");
        } catch (Exception ex) {
            log.error("[PROVISION] Error occurred during demo account provisioning: {}", ex.getMessage());
        }
    }

    @Transactional
    public void provisionDemoAccounts() {
        Role adminRole = getOrCreateRole("ADMIN");
        Role distributorRole = getOrCreateRole("DISTRIBUTOR");

        provisionAdminAccount(adminRole);
        provisionDistributorAccount(distributorRole);
    }

    private void provisionAdminAccount(Role adminRole) {
        String adminEmail = "admin@lpgchain.example";
        Optional<User> existingUser = userRepository.findByEmail(adminEmail);

        User admin;
        if (existingUser.isEmpty()) {
            admin = new User();
            admin.setEmail(adminEmail);
            admin.setName("LPG-Chain Administrator");
            admin.setPhone("+91 98201 00001");
            admin.setAddress("LPG-Chain Operations HQ, BKC, Mumbai");
            admin.setPassword(passwordService.encode("Admin@123"));
            admin = userRepository.save(admin);
            log.info("[PROVISION] Created missing demo Admin user: {}", adminEmail);
        } else {
            admin = existingUser.get();
            log.info("[PROVISION] Demo Admin user already exists ({}). Preserved existing credentials.", adminEmail);
        }

        ensureUserRole(admin, adminRole);
    }

    private void provisionDistributorAccount(Role distributorRole) {
        String distributorEmail = "distributor@agency.example";
        Distributor distributor = getOrCreateDistributorAgency();

        Optional<User> existingUser = userRepository.findByEmail(distributorEmail);
        User distUser;
        if (existingUser.isEmpty()) {
            distUser = new User();
            distUser.setEmail(distributorEmail);
            distUser.setName("Apex Gas Agency");
            distUser.setPhone("+91 98201 54321");
            distUser.setAddress("Shop 4, Market Yard, Sector 18, Vashi, Navi Mumbai, MH 400703");
            distUser.setPassword(passwordService.encode("123456"));
            distUser = userRepository.save(distUser);
            log.info("[PROVISION] Created missing demo Distributor user: {}", distributorEmail);
        } else {
            distUser = existingUser.get();
            log.info("[PROVISION] Demo Distributor user already exists ({}). Preserved existing credentials.", distributorEmail);
        }

        ensureUserRole(distUser, distributorRole);
        ensureDistributorMembership(distUser, distributor);
    }

    private Distributor getOrCreateDistributorAgency() {
        Optional<Distributor> byLicence = distributorRepository.findByLicenceNumber("LPG-MH-PN-1001");
        if (byLicence.isPresent()) {
            return byLicence.get();
        }

        List<Distributor> all = distributorRepository.findAll();
        if (!all.isEmpty()) {
            return all.get(0);
        }

        Distributor d = new Distributor();
        d.setAgencyName("Apex Gas Agency");
        d.setLicenceNumber("LPG-MH-PN-1001");
        d.setAddress("Shop 4, Market Yard, Sector 18, Vashi");
        d.setCity("Navi Mumbai");
        d.setPinCode("400703");
        d.setContactNumber("+91 98201 54321");
        d.setSupportedServices("BOTH");
        d.setStatus("ACTIVE");
        Distributor saved = distributorRepository.save(d);
        log.info("[PROVISION] Created default demo Distributor agency: {}", saved.getAgencyName());
        return saved;
    }

    private Role getOrCreateRole(String roleName) {
        return roleRepository.findByNameIgnoreCase(roleName).orElseGet(() -> {
            Role r = new Role(roleName);
            Role saved = roleRepository.save(r);
            log.info("[PROVISION] Created missing role: {}", roleName);
            return saved;
        });
    }

    private void ensureUserRole(User user, Role role) {
        List<UserRole> existing = userRoleRepository.findByUser(user);
        boolean hasRole = existing.stream()
                .anyMatch(ur -> ur.getRole() != null && role.getName().equalsIgnoreCase(ur.getRole().getName()));
        if (!hasRole) {
            UserRole ur = new UserRole(user, role);
            userRoleRepository.save(ur);
            log.info("[PROVISION] Linked role '{}' to user '{}'", role.getName(), user.getEmail());
        }
    }

    private void ensureDistributorMembership(User user, Distributor distributor) {
        if (distributor != null && !distributorMembershipRepository.existsByUserIdAndDistributorId(user.getId(), distributor.getId())) {
            DistributorMembership membership = new DistributorMembership(user, distributor, "OWNER", "ACTIVE");
            distributorMembershipRepository.save(membership);
            log.info("[PROVISION] Linked active OWNER membership for user '{}' to agency id={}", user.getEmail(), distributor.getId());
        }
    }
}
