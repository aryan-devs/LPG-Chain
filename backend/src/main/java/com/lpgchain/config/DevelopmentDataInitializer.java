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
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DevelopmentDataInitializer — Bootstraps essential development and test records
 * for the LPG-Chain platform: roles, distributor agency membership, fleet assets,
 * physical depot stock, monthly quota allocations, and operational order lifecycles.
 */
@Component
@Profile("!prod & !test")
public class DevelopmentDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentDataInitializer.class);

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

    public DevelopmentDataInitializer(RoleRepository roleRepository,
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
    @Transactional
    public void run(String... args) {
        log.info("Bootstrapping development environment test data...");

        // 1. Ensure Roles
        Role customerRole = getOrCreateRole("CUSTOMER");
        Role distributorRole = getOrCreateRole("DISTRIBUTOR");
        getOrCreateRole("ADMIN");

        // 2. Ensure Distributor Agency #1
        Distributor distributor = distributorRepository.findById(1L).orElseGet(() -> {
            Optional<Distributor> existing = distributorRepository.findAll().stream().findFirst();
            if (existing.isPresent()) {
                return existing.get();
            }
            Distributor d = new Distributor();
            d.setAgencyName("Sharma Bharat Gas Agency");
            d.setLicenceNumber("LPG-MH-PN-1001");
            d.setAddress("Shop 4, Market Yard, Sector 18, Vashi");
            d.setCity("Navi Mumbai");
            d.setPinCode("400703");
            d.setContactNumber("+91 98201 54321");
            d.setSupportedServices("BOTH");
            return distributorRepository.save(d);
        });

        // 3. Ensure Distributor User
        User distributorUser = userRepository.findByEmail("distributor@agency.example").orElseGet(() -> {
            User u = new User();
            u.setEmail("distributor@agency.example");
            u.setName("Apex Gas Agency");
            u.setPhone("+91 98201 54321");
            u.setAddress("Shop 4, Market Yard, Sector 18, Vashi, Navi Mumbai, MH 400703");
            u.setPassword(passwordService.encode("123456"));
            return userRepository.save(u);
        });

        if (distributorUser != null) {
            if (distributorUser.getPassword() == null || !passwordService.matches("123456", distributorUser.getPassword())) {
                distributorUser.setPassword(passwordService.encode("123456"));
                distributorUser = userRepository.save(distributorUser);
            }

            if (distributorUser != null) {
                ensureUserRole(distributorUser, distributorRole);

                if (distributor != null && !distributorMembershipRepository.existsByUserIdAndDistributorId(distributorUser.getId(), distributor.getId())) {
                    DistributorMembership membership = new DistributorMembership(distributorUser, distributor, "OWNER", "ACTIVE");
                    distributorMembershipRepository.save(membership);
                }
            }
        }

        // 4. Ensure Customer User
        User customerUser = userRepository.findByEmail("customer@example.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("customer@example.com");
            u.setName("Anand Verma");
            u.setPhone("+91 98201 54321");
            u.setAddress("Flat 402, Green Heights, Vashi, Navi Mumbai, MH 400703");
            u.setPassword(passwordService.encode("123456"));
            return userRepository.save(u);
        });

        if (customerUser != null) {
            if (customerUser.getPassword() == null || !passwordService.matches("123456", customerUser.getPassword())) {
                customerUser.setPassword(passwordService.encode("123456"));
                customerUser = userRepository.save(customerUser);
            }

            if (customerUser != null) {
                ensureUserRole(customerUser, customerRole);
            }
        }

        // 4b. Ensure Development Admin User
        Role adminRole = getOrCreateRole("ADMIN");
        User adminUser = userRepository.findByEmail("admin@lpgchain.example").orElseGet(() -> {
            User u = new User();
            u.setEmail("admin@lpgchain.example");
            u.setName("LPG-Chain Administrator");
            u.setPhone("+91 98201 00001");
            u.setAddress("LPG-Chain Operations HQ, BKC, Mumbai");
            u.setPassword(passwordService.encode("Admin@123"));
            return userRepository.save(u);
        });

        if (adminUser != null) {
            if (adminUser.getPassword() == null || !passwordService.matches("Admin@123", adminUser.getPassword())) {
                adminUser.setPassword(passwordService.encode("Admin@123"));
                adminUser = userRepository.save(adminUser);
            }
            if (adminUser != null) {
                ensureUserRole(adminUser, adminRole);
            }
        }

        // 5. Ensure Drivers & Vehicles
        if (distributor == null) {
            log.info("Distributor is null in mock environment; skipping dependent seed data.");
            return;
        }
        Driver driver1 = driverRepository.findByLicenseNumber("MH-14-2019-0012345").orElseGet(() -> {
            Driver d = new Driver("Suresh Patil", "+91 98765 43210", "MH-14-2019-0012345", "AVAILABLE", distributor);
            return driverRepository.save(d);
        });

        Driver driver2 = driverRepository.findByLicenseNumber("MH-14-2020-0067890").orElseGet(() -> {
            Driver d = new Driver("Ramesh Shinde", "+91 98765 43211", "MH-14-2020-0067890", "AVAILABLE", distributor);
            return driverRepository.save(d);
        });

        Vehicle vehicle1 = vehicleRepository.findByVehicleNumber("MH-12-RN-4501").orElseGet(() -> {
            Vehicle v = new Vehicle("MH-12-RN-4501", "MINI_TRUCK", 40, "AVAILABLE", distributor);
            return vehicleRepository.save(v);
        });

        Vehicle vehicle2 = vehicleRepository.findByVehicleNumber("MH-12-RN-4502").orElseGet(() -> {
            Vehicle v = new Vehicle("MH-12-RN-4502", "LIGHT_TRUCK", 60, "AVAILABLE", distributor);
            return vehicleRepository.save(v);
        });

        // 6. Ensure Depot Physical Stock
        ensureCylinderInventory(distributor, "DOMESTIC_14_2", 14.2, 60, 20, 1);
        ensureCylinderInventory(distributor, "DOMESTIC_5_0", 5.0, 30, 10, 0);
        ensureCylinderInventory(distributor, "COMMERCIAL_19_0", 19.0, 40, 15, 0);

        // 7. Ensure Quota Allocations
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();
        ensureAllocation(distributor, "DOMESTIC_14_2", 14.2, 1200, year, month);
        ensureAllocation(distributor, "COMMERCIAL_19_0", 19.0, 500, year, month);
        ensureAllocation(distributor, "DOMESTIC_5_0", 5.0, 300, year, month);

        // 8. Ensure Operational Bookings and Deliveries
        if (customerUser != null && distributor != null && driver1 != null && driver2 != null && vehicle1 != null && vehicle2 != null) {
            ensureOperationalOrders(customerUser, distributor, driver1, driver2, vehicle1, vehicle2);
        }

        log.info("Development test data initialization completed successfully.");
    }

    private Role getOrCreateRole(String roleName) {
        return roleRepository.findByName(roleName).orElseGet(() -> {
            Role r = new Role(roleName);
            return roleRepository.save(r);
        });
    }

    private void ensureUserRole(User user, Role role) {
        List<UserRole> existing = userRoleRepository.findByUser(user);
        boolean hasRole = existing.stream()
                .anyMatch(ur -> ur.getRole() != null && role.getName().equalsIgnoreCase(ur.getRole().getName()));
        if (!hasRole) {
            UserRole ur = new UserRole(user, role);
            userRoleRepository.save(ur);
        }
    }

    private void ensureCylinderInventory(Distributor distributor, String category, Double capacityKg,
                                         int filled, int empty, int damaged) {
        Optional<CylinderInventory> opt = inventoryRepository.findByDistributorIdAndCylinderCategory(distributor.getId(), category);
        if (opt.isEmpty()) {
            CylinderInventory inv = new CylinderInventory(distributor, category, capacityKg, filled, empty, 0, damaged, "DEV_INIT");
            CylinderInventory saved = inventoryRepository.save(inv);
            InventoryMovement mov = new InventoryMovement(saved, "STOCK_RECEIPT", filled,
                    "Initial development depot stock setup", "DEV-INIT-" + category, "DEV_INIT");
            movementRepository.save(mov);
        }
    }

    private void ensureAllocation(Distributor distributor, String category, Double capacityKg,
                                 int quantity, int year, int month) {
        Optional<CylinderAllocation> opt = allocationRepository
                .findByDistributorIdAndCylinderCategoryAndReportingYearAndReportingMonth(distributor.getId(), category, year, month);
        if (opt.isEmpty()) {
            CylinderAllocation alloc = new CylinderAllocation(distributor, category, capacityKg, quantity, year, month,
                    LocalDate.of(year, month, 1), "DEV-TEST-ALLOC-" + year + "-" + String.format("%02d", month), "DEV_INIT");
            allocationRepository.save(alloc);
        }
    }

    private void ensureOperationalOrders(User customer, Distributor distributor,
                                        Driver driver1, Driver driver2,
                                        Vehicle vehicle1, Vehicle vehicle2) {
        List<Delivery> existingDeliveries = deliveryRepository.findByDistributorId(distributor.getId());
        if (!existingDeliveries.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Queued order waiting for dispatch (CONFIRMED)
        Booking bookingQueued = new Booking(customer, distributor, customer.getAddress(), now.minusHours(4), 1, "CONFIRMED");
        bookingQueued.setCylinderCategory("DOMESTIC_14_2");
        bookingQueued.setCapacityKg(14.2);
        bookingRepository.save(bookingQueued);

        // 2. Scheduled delivery (ASSIGNED)
        Booking bookingAssigned = new Booking(customer, distributor, customer.getAddress(), now.minusHours(3), 1, "CONFIRMED");
        bookingAssigned.setCylinderCategory("DOMESTIC_14_2");
        bookingAssigned.setCapacityKg(14.2);
        Booking savedAssignedBooking = bookingRepository.save(bookingAssigned);

        Delivery deliveryAssigned = new Delivery(savedAssignedBooking, driver1, vehicle1, distributor, "ASSIGNED");
        deliveryAssigned.setAssignedAt(now.minusHours(2));
        deliveryRepository.save(deliveryAssigned);

        // 3. Dispatched delivery in transit (OUT_FOR_DELIVERY) with OTP
        Booking bookingOut = new Booking(customer, distributor, "Plot 18, Commercial Zone, Navi Mumbai", now.minusHours(3), 1, "CONFIRMED");
        bookingOut.setCylinderCategory("COMMERCIAL_19_0");
        bookingOut.setCapacityKg(19.0);
        Booking savedOutBooking = bookingRepository.save(bookingOut);

        Delivery deliveryOut = new Delivery(savedOutBooking, driver2, vehicle2, distributor, "OUT_FOR_DELIVERY");
        deliveryOut.setAssignedAt(now.minusHours(3));
        deliveryOut.setPickedUpAt(now.minusHours(2));
        deliveryOut.setOutForDeliveryAt(now.minusHours(1));
        deliveryOut.setOtpHash(passwordEncoder.encode("849201"));
        deliveryOut.setOtpExpiresAt(now.plusMinutes(15));
        deliveryOut.setOtpAttempts(0);
        deliveryOut.setOtpMaxAttempts(3);
        deliveryOut.setOtpGeneratedAt(now.minusMinutes(5));
        deliveryRepository.save(deliveryOut);

        driver2.setStatus("BUSY");
        driverRepository.save(driver2);
        vehicle2.setStatus("IN_TRANSIT");
        vehicleRepository.save(vehicle2);

        // 4. Completed delivery (DELIVERED)
        Booking bookingDelivered = new Booking(customer, distributor, customer.getAddress(), now.minusDays(1), 1, "DELIVERED");
        bookingDelivered.setCylinderCategory("DOMESTIC_14_2");
        bookingDelivered.setCapacityKg(14.2);
        Booking savedDeliveredBooking = bookingRepository.save(bookingDelivered);

        Delivery deliveryDelivered = new Delivery(savedDeliveredBooking, driver1, vehicle1, distributor, "DELIVERED");
        deliveryDelivered.setAssignedAt(now.minusDays(1));
        deliveryDelivered.setPickedUpAt(now.minusDays(1));
        deliveryDelivered.setOutForDeliveryAt(now.minusDays(1));
        deliveryDelivered.setDeliveredAt(now.minusDays(1));
        deliveryDelivered.setOtpVerifiedAt(now.minusDays(1));
        deliveryRepository.save(deliveryDelivered);
    }
}
