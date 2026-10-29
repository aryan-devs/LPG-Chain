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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionDemoAccountProvisionerTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private DistributorRepository distributorRepository;

    @Mock
    private DistributorMembershipRepository distributorMembershipRepository;

    @Mock
    private PasswordService passwordService;

    private Role adminRole;
    private Role distributorRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role(1L, "ADMIN");
        distributorRole = new Role(2L, "DISTRIBUTOR");
    }

    @Test
    @DisplayName("PROVISION: When PROVISION_DEMO_ACCOUNTS=false, provisioner should skip without performing queries or inserts")
    void shouldSkipWhenDisabled() {
        ProductionDemoAccountProvisioner provisioner = new ProductionDemoAccountProvisioner(
                false,
                roleRepository,
                userRepository,
                userRoleRepository,
                distributorRepository,
                distributorMembershipRepository,
                passwordService
        );

        provisioner.run();

        verify(roleRepository, never()).findByNameIgnoreCase(anyString());
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(distributorRepository, never()).save(any(Distributor.class));
    }

    @Test
    @DisplayName("PROVISION: When PROVISION_DEMO_ACCOUNTS=true and accounts are absent, provisions Admin, Distributor, Agency, and Membership")
    void shouldProvisionAccountsWhenEnabledAndMissing() {
        ProductionDemoAccountProvisioner provisioner = new ProductionDemoAccountProvisioner(
                true,
                roleRepository,
                userRepository,
                userRoleRepository,
                distributorRepository,
                distributorMembershipRepository,
                passwordService
        );

        when(roleRepository.findByNameIgnoreCase("ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByNameIgnoreCase("DISTRIBUTOR")).thenReturn(Optional.of(distributorRole));

        when(userRepository.findByEmail("admin@lpgchain.example")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("distributor@agency.example")).thenReturn(Optional.empty());

        when(passwordService.encode("Admin@123")).thenReturn("peppered_bcrypt_hash_admin");
        when(passwordService.encode("123456")).thenReturn("peppered_bcrypt_hash_distributor");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        Distributor mockAgency = new Distributor();
        mockAgency.setId(100L);
        mockAgency.setAgencyName("Apex Gas Agency");
        mockAgency.setLicenceNumber("LPG-MH-PN-1001");
        when(distributorRepository.findByLicenceNumber("LPG-MH-PN-1001")).thenReturn(Optional.of(mockAgency));

        when(userRoleRepository.findByUser(any(User.class))).thenReturn(Collections.emptyList());
        when(distributorMembershipRepository.existsByUserIdAndDistributorId(anyLong(), anyLong())).thenReturn(false);

        provisioner.run();

        // 1. Verify Admin user creation
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());
        List<User> savedUsers = userCaptor.getAllValues();

        User savedAdmin = savedUsers.stream()
                .filter(u -> "admin@lpgchain.example".equals(u.getEmail()))
                .findFirst()
                .orElse(null);
        assertNotNull(savedAdmin);
        assertEquals("peppered_bcrypt_hash_admin", savedAdmin.getPassword());
        assertEquals("LPG-Chain Administrator", savedAdmin.getName());

        // 2. Verify Distributor user creation
        User savedDistributor = savedUsers.stream()
                .filter(u -> "distributor@agency.example".equals(u.getEmail()))
                .findFirst()
                .orElse(null);
        assertNotNull(savedDistributor);
        assertEquals("peppered_bcrypt_hash_distributor", savedDistributor.getPassword());
        assertEquals("Apex Gas Agency", savedDistributor.getName());

        // 3. Verify roles assigned
        verify(userRoleRepository, times(2)).save(any(UserRole.class));

        // 4. Verify DistributorMembership assigned with OWNER and ACTIVE
        ArgumentCaptor<DistributorMembership> membershipCaptor = ArgumentCaptor.forClass(DistributorMembership.class);
        verify(distributorMembershipRepository, times(1)).save(membershipCaptor.capture());
        DistributorMembership savedMembership = membershipCaptor.getValue();
        assertEquals("OWNER", savedMembership.getMembershipRole());
        assertEquals("ACTIVE", savedMembership.getStatus());
    }

    @Test
    @DisplayName("PROVISION: When accounts already exist, idempotent logic does not overwrite passwords or duplicate mappings")
    void shouldBeIdempotentWhenAccountsAlreadyExist() {
        ProductionDemoAccountProvisioner provisioner = new ProductionDemoAccountProvisioner(
                true,
                roleRepository,
                userRepository,
                userRoleRepository,
                distributorRepository,
                distributorMembershipRepository,
                passwordService
        );

        when(roleRepository.findByNameIgnoreCase("ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByNameIgnoreCase("DISTRIBUTOR")).thenReturn(Optional.of(distributorRole));

        User existingAdmin = new User(1L, "Existing Admin", "admin@lpgchain.example", "existing_secure_hash");
        User existingDist = new User(2L, "Existing Distributor", "distributor@agency.example", "existing_dist_hash");

        when(userRepository.findByEmail("admin@lpgchain.example")).thenReturn(Optional.of(existingAdmin));
        when(userRepository.findByEmail("distributor@agency.example")).thenReturn(Optional.of(existingDist));

        Distributor existingAgency = new Distributor();
        existingAgency.setId(1L);
        when(distributorRepository.findByLicenceNumber("LPG-MH-PN-1001")).thenReturn(Optional.of(existingAgency));

        when(userRoleRepository.findByUser(existingAdmin)).thenReturn(List.of(new UserRole(existingAdmin, adminRole)));
        when(userRoleRepository.findByUser(existingDist)).thenReturn(List.of(new UserRole(existingDist, distributorRole)));
        when(distributorMembershipRepository.existsByUserIdAndDistributorId(2L, 1L)).thenReturn(true);

        provisioner.run();

        // Passwords must never be re-encoded or overwritten
        verify(passwordService, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(userRoleRepository, never()).save(any(UserRole.class));
        verify(distributorMembershipRepository, never()).save(any(DistributorMembership.class));
    }

    @Test
    @DisplayName("PROVISION: Gracefully catches and logs unexpected repository failures without throwing unhandled exceptions")
    void shouldHandleFailuresGracefully() {
        ProductionDemoAccountProvisioner provisioner = new ProductionDemoAccountProvisioner(
                true,
                roleRepository,
                userRepository,
                userRoleRepository,
                distributorRepository,
                distributorMembershipRepository,
                passwordService
        );

        when(roleRepository.findByNameIgnoreCase(anyString())).thenThrow(new RuntimeException("Database connectivity timeout"));

        assertDoesNotThrow(() -> provisioner.run());
    }
}
