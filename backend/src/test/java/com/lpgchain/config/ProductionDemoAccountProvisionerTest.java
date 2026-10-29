package com.lpgchain.config;

import com.lpgchain.entity.Booking;
import com.lpgchain.entity.CylinderAllocation;
import com.lpgchain.entity.CylinderInventory;
import com.lpgchain.entity.Delivery;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Driver;
import com.lpgchain.entity.InventoryMovement;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.CylinderAllocationRepository;
import com.lpgchain.repository.CylinderInventoryRepository;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.DriverRepository;
import com.lpgchain.repository.InventoryMovementRepository;
import com.lpgchain.repository.RoleRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.repository.VehicleRepository;
import com.lpgchain.security.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CylinderInventoryRepository inventoryRepository;

    @Mock
    private InventoryMovementRepository movementRepository;

    @Mock
    private CylinderAllocationRepository allocationRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private PasswordService passwordService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Role adminRole;
    private Role distributorRole;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role(1L, "ADMIN");
        distributorRole = new Role(2L, "DISTRIBUTOR");
        customerRole = new Role(3L, "CUSTOMER");
    }

    private ProductionDemoAccountProvisioner createProvisioner(boolean enabled) {
        return new ProductionDemoAccountProvisioner(
                enabled,
                roleRepository,
                userRepository,
                userRoleRepository,
                distributorRepository,
                distributorMembershipRepository,
                driverRepository,
                vehicleRepository,
                inventoryRepository,
                movementRepository,
                allocationRepository,
                bookingRepository,
                deliveryRepository,
                passwordService,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("PROVISION: When PROVISION_DEMO_ACCOUNTS=false, provisioner should skip without performing queries or inserts")
    void shouldSkipWhenDisabled() {
        ProductionDemoAccountProvisioner provisioner = createProvisioner(false);

        provisioner.run();

        verify(roleRepository, never()).findByNameIgnoreCase(anyString());
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(distributorRepository, never()).save(any(Distributor.class));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("PROVISION: When PROVISION_DEMO_ACCOUNTS=true and accounts are absent, provisions Admin, Distributor, Customer, Fleet, Inventory, Allocations, and Orders")
    void shouldProvisionAccountsWhenEnabledAndMissing() {
        ProductionDemoAccountProvisioner provisioner = createProvisioner(true);

        when(roleRepository.findByNameIgnoreCase("ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByNameIgnoreCase("DISTRIBUTOR")).thenReturn(Optional.of(distributorRole));
        when(roleRepository.findByNameIgnoreCase("CUSTOMER")).thenReturn(Optional.of(customerRole));

        when(userRepository.findByEmail("admin@lpgchain.example")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("distributor@agency.example")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.empty());

        when(passwordService.encode("Admin@123")).thenReturn("peppered_bcrypt_hash_admin");
        when(passwordService.encode("123456")).thenReturn("peppered_bcrypt_hash_distributor");
        when(passwordEncoder.encode("849201")).thenReturn("bcrypt_otp_hash");

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

        // Drivers & Vehicles
        Driver d1 = new Driver("Suresh Patil", "+91 98765 43210", "MH-14-2019-0012345", "AVAILABLE", mockAgency);
        d1.setId(1L);
        Driver d2 = new Driver("Ramesh Shinde", "+91 98765 43211", "MH-14-2020-0067890", "AVAILABLE", mockAgency);
        d2.setId(2L);
        Vehicle v1 = new Vehicle("MH-12-RN-4501", "MINI_TRUCK", 40, "AVAILABLE", mockAgency);
        v1.setId(1L);
        Vehicle v2 = new Vehicle("MH-12-RN-4502", "LIGHT_TRUCK", 60, "AVAILABLE", mockAgency);
        v2.setId(2L);

        when(driverRepository.findByLicenseNumber("MH-14-2019-0012345")).thenReturn(Optional.of(d1));
        when(driverRepository.findByLicenseNumber("MH-14-2020-0067890")).thenReturn(Optional.of(d2));
        when(vehicleRepository.findByVehicleNumber("MH-12-RN-4501")).thenReturn(Optional.of(v1));
        when(vehicleRepository.findByVehicleNumber("MH-12-RN-4502")).thenReturn(Optional.of(v2));

        // Inventory
        when(inventoryRepository.findByDistributorIdAndCylinderCategory(anyLong(), anyString())).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(CylinderInventory.class))).thenAnswer(i -> i.getArgument(0));

        // Allocations
        when(allocationRepository.findByDistributorIdAndCylinderCategoryAndReportingYearAndReportingMonth(anyLong(), anyString(), anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        // Bookings
        when(bookingRepository.findByDistributorId(100L)).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId(500L);
            return b;
        });

        provisioner.run();

        // 1. Verify 3 users created: Admin, Distributor, Customer
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(3)).save(userCaptor.capture());
        List<User> savedUsers = userCaptor.getAllValues();

        User savedAdmin = savedUsers.stream()
                .filter(u -> "admin@lpgchain.example".equals(u.getEmail()))
                .findFirst()
                .orElse(null);
        assertNotNull(savedAdmin);
        assertEquals("peppered_bcrypt_hash_admin", savedAdmin.getPassword());

        User savedDistributor = savedUsers.stream()
                .filter(u -> "distributor@agency.example".equals(u.getEmail()))
                .findFirst()
                .orElse(null);
        assertNotNull(savedDistributor);

        User savedCustomer = savedUsers.stream()
                .filter(u -> "customer@example.com".equals(u.getEmail()))
                .findFirst()
                .orElse(null);
        assertNotNull(savedCustomer);

        // 2. Verify roles assigned for 3 users
        verify(userRoleRepository, times(3)).save(any(UserRole.class));

        // 3. Verify DistributorMembership assigned
        verify(distributorMembershipRepository, times(1)).save(any(DistributorMembership.class));

        // 4. Verify Inventory seeded (3 categories)
        verify(inventoryRepository, times(3)).save(any(CylinderInventory.class));
        verify(movementRepository, times(3)).save(any(InventoryMovement.class));

        // 5. Verify Allocations seeded (3 categories)
        verify(allocationRepository, times(3)).save(any(CylinderAllocation.class));

        // 6. Verify Operational Bookings seeded (5 bookings: pending, queued, assigned, out, delivered)
        verify(bookingRepository, times(5)).save(any(Booking.class));

        // 7. Verify Deliveries seeded (3 deliveries: assigned, out, delivered)
        verify(deliveryRepository, times(3)).save(any(Delivery.class));
    }

    @Test
    @DisplayName("PROVISION: When accounts and bookings already exist, idempotent logic does not overwrite passwords or duplicate bookings")
    void shouldBeIdempotentWhenAccountsAlreadyExist() {
        ProductionDemoAccountProvisioner provisioner = createProvisioner(true);

        when(roleRepository.findByNameIgnoreCase("ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByNameIgnoreCase("DISTRIBUTOR")).thenReturn(Optional.of(distributorRole));
        when(roleRepository.findByNameIgnoreCase("CUSTOMER")).thenReturn(Optional.of(customerRole));

        User existingAdmin = new User(1L, "Existing Admin", "admin@lpgchain.example", "existing_secure_hash");
        User existingDist = new User(2L, "Existing Distributor", "distributor@agency.example", "existing_dist_hash");
        User existingCust = new User(3L, "Existing Customer", "customer@example.com", "existing_cust_hash");

        when(userRepository.findByEmail("admin@lpgchain.example")).thenReturn(Optional.of(existingAdmin));
        when(userRepository.findByEmail("distributor@agency.example")).thenReturn(Optional.of(existingDist));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(existingCust));

        Distributor existingAgency = new Distributor();
        existingAgency.setId(1L);
        when(distributorRepository.findByLicenceNumber("LPG-MH-PN-1001")).thenReturn(Optional.of(existingAgency));

        when(userRoleRepository.findByUser(existingAdmin)).thenReturn(List.of(new UserRole(existingAdmin, adminRole)));
        when(userRoleRepository.findByUser(existingDist)).thenReturn(List.of(new UserRole(existingDist, distributorRole)));
        when(userRoleRepository.findByUser(existingCust)).thenReturn(List.of(new UserRole(existingCust, customerRole)));
        when(distributorMembershipRepository.existsByUserIdAndDistributorId(2L, 1L)).thenReturn(true);

        // Existing drivers & vehicles
        when(driverRepository.findByLicenseNumber(anyString())).thenReturn(Optional.of(new Driver()));
        when(vehicleRepository.findByVehicleNumber(anyString())).thenReturn(Optional.of(new Vehicle()));

        // Existing inventory with non-zero stock
        CylinderInventory existingInv = new CylinderInventory(existingAgency, "DOMESTIC_14_2", 14.2, 50, 10, 0, 0, "SYSTEM");
        when(inventoryRepository.findByDistributorIdAndCylinderCategory(anyLong(), anyString())).thenReturn(Optional.of(existingInv));

        // Existing allocations
        when(allocationRepository.findByDistributorIdAndCylinderCategoryAndReportingYearAndReportingMonth(anyLong(), anyString(), anyInt(), anyInt()))
                .thenReturn(Optional.of(new CylinderAllocation()));

        // Existing bookings present
        when(bookingRepository.findByDistributorId(1L)).thenReturn(List.of(new Booking()));

        provisioner.run();

        // Passwords must never be re-encoded or overwritten
        verify(passwordService, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(userRoleRepository, never()).save(any(UserRole.class));
        verify(distributorMembershipRepository, never()).save(any(DistributorMembership.class));

        // Bookings & Deliveries must never be duplicated
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(deliveryRepository, never()).save(any(Delivery.class));
    }

    @Test
    @DisplayName("PROVISION: Gracefully catches and logs unexpected repository failures without throwing unhandled exceptions")
    void shouldHandleFailuresGracefully() {
        ProductionDemoAccountProvisioner provisioner = createProvisioner(true);

        when(roleRepository.findByNameIgnoreCase(anyString())).thenThrow(new RuntimeException("Database connectivity timeout"));

        assertDoesNotThrow(() -> provisioner.run());
    }
}
