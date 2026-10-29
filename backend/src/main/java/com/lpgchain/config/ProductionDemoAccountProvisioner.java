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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ProductionDemoAccountProvisioner — Opt-in, idempotent bootstrapper for initial
 * demo accounts, fleet assets, depot stock, monthly quota allocations, and sample
 * operational orders in production cloud databases (e.g. TiDB Cloud).
 *
 * Activation:
 *  Strictly gated by `lpgchain.provision-demo-accounts=true` (env var `PROVISION_DEMO_ACCOUNTS=true`).
 *  Defaults to `false`. Does not expose any HTTP endpoint.
 *
 * Guarantees:
 *  1. Checks `findByEmail` before inserting. Never overwrites or resets existing users or passwords.
 *  2. Uses application-level {@link PasswordService} to encode credentials with the production pepper.
 *  3. Assigns verified ADMIN, DISTRIBUTOR, and CUSTOMER roles via `user_role` mapping.
 *  4. Associates the Distributor user with an agency and creates an ACTIVE DistributorMembership.
 *  5. Seeds physical inventory stock only if categories are missing or have zero stock.
 *  6. Seeds demo operational bookings and deliveries only if no prior bookings exist for the agency.
 *  7. Never logs raw passwords, password hashes, or pepper secrets.
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
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final CylinderInventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final CylinderAllocationRepository allocationRepository;
    private final BookingRepository bookingRepository;
    private final DeliveryRepository deliveryRepository;
    private final PasswordService passwordService;
    private final PasswordEncoder passwordEncoder;

    public ProductionDemoAccountProvisioner(
            @Value("${lpgchain.provision-demo-accounts:false}") boolean provisionDemoAccountsEnabled,
            RoleRepository roleRepository,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            DistributorRepository distributorRepository,
            DistributorMembershipRepository distributorMembershipRepository,
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository,
            CylinderInventoryRepository inventoryRepository,
            InventoryMovementRepository movementRepository,
            CylinderAllocationRepository allocationRepository,
            BookingRepository bookingRepository,
            DeliveryRepository deliveryRepository,
            PasswordService passwordService,
            PasswordEncoder passwordEncoder) {
        this.provisionDemoAccountsEnabled = provisionDemoAccountsEnabled;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.distributorRepository = distributorRepository;
        this.distributorMembershipRepository = distributorMembershipRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.allocationRepository = allocationRepository;
        this.bookingRepository = bookingRepository;
        this.deliveryRepository = deliveryRepository;
        this.passwordService = passwordService;
        this.passwordEncoder = passwordEncoder;
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
            log.error("[PROVISION] Error occurred during demo account provisioning: {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    public void provisionDemoAccounts() {
        Role adminRole = getOrCreateRole("ADMIN");
        Role distributorRole = getOrCreateRole("DISTRIBUTOR");
        Role customerRole = getOrCreateRole("CUSTOMER");

        provisionAdminAccount(adminRole);
        Distributor distributor = provisionDistributorAccount(distributorRole);
        User customer = provisionDemoCustomer(customerRole);

        if (distributor != null) {
            provisionDistributorFleet(distributor);
            provisionDepotInventory(distributor);
            provisionQuotaAllocations(distributor);

            if (customer != null) {
                provisionOperationalOrders(distributor, customer);
            }
        }
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

    private Distributor provisionDistributorAccount(Role distributorRole) {
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
        return distributor;
    }

    private User provisionDemoCustomer(Role customerRole) {
        String customerEmail = "customer@example.com";
        Optional<User> existing = userRepository.findByEmail(customerEmail);
        User customer;
        if (existing.isEmpty()) {
            customer = new User();
            customer.setEmail(customerEmail);
            customer.setName("Anand Verma");
            customer.setPhone("+91 98201 54321");
            customer.setAddress("Flat 402, Green Heights, Vashi, Navi Mumbai, MH 400703");
            customer.setPassword(passwordService.encode("123456"));
            customer = userRepository.save(customer);
            log.info("[PROVISION] Created demo Customer user: {}", customerEmail);
        } else {
            customer = existing.get();
            log.info("[PROVISION] Demo Customer user already exists ({}). Preserved existing credentials.", customerEmail);
        }
        ensureUserRole(customer, customerRole);
        return customer;
    }

    private void provisionDistributorFleet(Distributor distributor) {
        driverRepository.findByLicenseNumber("MH-14-2019-0012345").orElseGet(() -> {
            Driver d = new Driver("Suresh Patil", "+91 98765 43210", "MH-14-2019-0012345", "AVAILABLE", distributor);
            log.info("[PROVISION] Created demo driver: {}", d.getName());
            return driverRepository.save(d);
        });

        driverRepository.findByLicenseNumber("MH-14-2020-0067890").orElseGet(() -> {
            Driver d = new Driver("Ramesh Shinde", "+91 98765 43211", "MH-14-2020-0067890", "AVAILABLE", distributor);
            log.info("[PROVISION] Created demo driver: {}", d.getName());
            return driverRepository.save(d);
        });

        vehicleRepository.findByVehicleNumber("MH-12-RN-4501").orElseGet(() -> {
            Vehicle v = new Vehicle("MH-12-RN-4501", "MINI_TRUCK", 40, "AVAILABLE", distributor);
            log.info("[PROVISION] Created demo vehicle: {}", v.getVehicleNumber());
            return vehicleRepository.save(v);
        });

        vehicleRepository.findByVehicleNumber("MH-12-RN-4502").orElseGet(() -> {
            Vehicle v = new Vehicle("MH-12-RN-4502", "LIGHT_TRUCK", 60, "AVAILABLE", distributor);
            log.info("[PROVISION] Created demo vehicle: {}", v.getVehicleNumber());
            return vehicleRepository.save(v);
        });
    }

    private void provisionDepotInventory(Distributor distributor) {
        ensureCylinderInventory(distributor, "DOMESTIC_14_2", 14.2, 60, 20, 1);
        ensureCylinderInventory(distributor, "DOMESTIC_5_0", 5.0, 30, 10, 0);
        ensureCylinderInventory(distributor, "COMMERCIAL_19_0", 19.0, 40, 15, 0);
    }

    private void ensureCylinderInventory(Distributor distributor, String category, Double capacityKg,
                                         int filled, int empty, int damaged) {
        Optional<CylinderInventory> opt = inventoryRepository.findByDistributorIdAndCylinderCategory(distributor.getId(), category);
        if (opt.isEmpty()) {
            CylinderInventory inv = new CylinderInventory(distributor, category, capacityKg, filled, empty, 0, damaged, "PROV_INIT");
            CylinderInventory saved = inventoryRepository.save(inv);
            InventoryMovement mov = new InventoryMovement(saved, "STOCK_RECEIPT", filled,
                    "Initial demo depot stock setup", "DEMO-INIT-" + category, "PROV_INIT");
            movementRepository.save(mov);
            log.info("[PROVISION] Created demo depot inventory for {}: {} filled, {} empty", category, filled, empty);
        } else {
            CylinderInventory inv = opt.get();
            if (inv.getFilledStock() == 0 && inv.getEmptyStock() == 0 && inv.getReservedStock() == 0) {
                inv.setFilledStock(filled);
                inv.setEmptyStock(empty);
                inv.setDamagedStock(damaged);
                inv.setLastAuditedBy("PROV_INIT");
                inv.setLastAuditedAt(LocalDateTime.now());
                CylinderInventory saved = inventoryRepository.save(inv);
                InventoryMovement mov = new InventoryMovement(saved, "STOCK_RECEIPT", filled,
                        "Initial demo depot stock replenishment", "DEMO-INIT-" + category, "PROV_INIT");
                movementRepository.save(mov);
                log.info("[PROVISION] Updated initial zero depot stock for {}: {} filled, {} empty", category, filled, empty);
            }
        }
    }

    private void provisionQuotaAllocations(Distributor distributor) {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        ensureAllocation(distributor, "DOMESTIC_14_2", 14.2, 1200, year, month);
        ensureAllocation(distributor, "COMMERCIAL_19_0", 19.0, 500, year, month);
        ensureAllocation(distributor, "DOMESTIC_5_0", 5.0, 300, year, month);
    }

    private void ensureAllocation(Distributor distributor, String category, Double capacityKg,
                                   int quantity, int year, int month) {
        Optional<CylinderAllocation> opt = allocationRepository
                .findByDistributorIdAndCylinderCategoryAndReportingYearAndReportingMonth(distributor.getId(), category, year, month);
        if (opt.isEmpty()) {
            CylinderAllocation alloc = new CylinderAllocation(distributor, category, capacityKg, quantity, year, month,
                    LocalDate.of(year, month, 1), "GOV-ALLOC-" + year + "-" + String.format("%02d", month), "PROV_INIT");
            allocationRepository.save(alloc);
            log.info("[PROVISION] Created demo monthly quota allocation for {}: {} cylinders ({}-{})", category, quantity, year, month);
        }
    }

    private void provisionOperationalOrders(Distributor distributor, User customer) {
        List<Booking> existing = bookingRepository.findByDistributorId(distributor.getId());
        if (!existing.isEmpty()) {
            log.info("[PROVISION] Distributor agency id={} already has {} bookings. Skipping operational orders seeding.",
                    distributor.getId(), existing.size());
            return;
        }

        Driver driver1 = driverRepository.findByLicenseNumber("MH-14-2019-0012345").orElse(null);
        Driver driver2 = driverRepository.findByLicenseNumber("MH-14-2020-0067890").orElse(null);
        Vehicle vehicle1 = vehicleRepository.findByVehicleNumber("MH-12-RN-4501").orElse(null);
        Vehicle vehicle2 = vehicleRepository.findByVehicleNumber("MH-12-RN-4502").orElse(null);

        if (driver1 == null || driver2 == null || vehicle1 == null || vehicle2 == null) {
            log.warn("[PROVISION] Drivers or vehicles not found for agency id={}. Skipping operational orders seeding.", distributor.getId());
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Pending booking request (New Requests count = 1)
        Booking bookingPending = new Booking(customer, distributor, customer.getAddress(), now.minusHours(2), 1, "PENDING");
        bookingPending.setCylinderCategory("DOMESTIC_14_2");
        bookingPending.setCapacityKg(14.2);
        bookingPending.setUnitPrice(903.0);
        bookingPending.setTotalAmount(903.0);
        bookingPending.setNetAmount(903.0);
        bookingRepository.save(bookingPending);

        // 2. Queued order waiting for dispatch (Accepted Queue count = 1)
        Booking bookingQueued = new Booking(customer, distributor, customer.getAddress(), now.minusHours(4), 1, "CONFIRMED");
        bookingQueued.setCylinderCategory("DOMESTIC_14_2");
        bookingQueued.setCapacityKg(14.2);
        bookingQueued.setUnitPrice(903.0);
        bookingQueued.setTotalAmount(903.0);
        bookingQueued.setNetAmount(903.0);
        bookingRepository.save(bookingQueued);

        // 3. Scheduled delivery dispatch (Active Dispatches includes ASSIGNED)
        Booking bookingAssigned = new Booking(customer, distributor, customer.getAddress(), now.minusHours(3), 1, "CONFIRMED");
        bookingAssigned.setCylinderCategory("DOMESTIC_14_2");
        bookingAssigned.setCapacityKg(14.2);
        bookingAssigned.setUnitPrice(903.0);
        bookingAssigned.setTotalAmount(903.0);
        bookingAssigned.setNetAmount(903.0);
        Booking savedAssignedBooking = bookingRepository.save(bookingAssigned);

        Delivery deliveryAssigned = new Delivery(savedAssignedBooking, driver1, vehicle1, distributor, "ASSIGNED");
        deliveryAssigned.setAssignedAt(now.minusHours(2));
        deliveryRepository.save(deliveryAssigned);

        // 4. In-transit delivery with OTP (Active Dispatches includes OUT_FOR_DELIVERY)
        Booking bookingOut = new Booking(customer, distributor, "Plot 18, Commercial Zone, Navi Mumbai", now.minusHours(3), 1, "CONFIRMED");
        bookingOut.setCylinderCategory("COMMERCIAL_19_0");
        bookingOut.setCapacityKg(19.0);
        bookingOut.setUnitPrice(1850.0);
        bookingOut.setTotalAmount(1850.0);
        bookingOut.setNetAmount(1850.0);
        Booking savedOutBooking = bookingRepository.save(bookingOut);

        Delivery deliveryOut = new Delivery(savedOutBooking, driver2, vehicle2, distributor, "OUT_FOR_DELIVERY");
        deliveryOut.setAssignedAt(now.minusHours(3));
        deliveryOut.setPickedUpAt(now.minusHours(2));
        deliveryOut.setOutForDeliveryAt(now.minusHours(1));
        deliveryOut.setOtpHash(passwordEncoder.encode("849201"));
        deliveryOut.setOtpExpiresAt(now.plusMinutes(60));
        deliveryOut.setOtpAttempts(0);
        deliveryOut.setOtpMaxAttempts(3);
        deliveryOut.setOtpGeneratedAt(now.minusMinutes(5));
        deliveryRepository.save(deliveryOut);

        driver2.setStatus("BUSY");
        driverRepository.save(driver2);
        vehicle2.setStatus("IN_TRANSIT");
        vehicleRepository.save(vehicle2);

        // 5. Completed sale (Completed Sales count = 1)
        Booking bookingDelivered = new Booking(customer, distributor, customer.getAddress(), now.minusDays(1), 1, "DELIVERED");
        bookingDelivered.setCylinderCategory("DOMESTIC_14_2");
        bookingDelivered.setCapacityKg(14.2);
        bookingDelivered.setUnitPrice(903.0);
        bookingDelivered.setTotalAmount(903.0);
        bookingDelivered.setNetAmount(903.0);
        Booking savedDeliveredBooking = bookingRepository.save(bookingDelivered);

        Delivery deliveryDelivered = new Delivery(savedDeliveredBooking, driver1, vehicle1, distributor, "DELIVERED");
        deliveryDelivered.setAssignedAt(now.minusDays(1));
        deliveryDelivered.setPickedUpAt(now.minusDays(1));
        deliveryDelivered.setOutForDeliveryAt(now.minusDays(1));
        deliveryDelivered.setDeliveredAt(now.minusDays(1));
        deliveryDelivered.setOtpVerifiedAt(now.minusDays(1));
        deliveryRepository.save(deliveryDelivered);

        log.info("[PROVISION] Seeded demo operational orders and deliveries for agency id={}", distributor.getId());
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
