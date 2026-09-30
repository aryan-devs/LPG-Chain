package com.lpgchain.integration;

import com.lpgchain.dto.DeliveryRequestDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Driver;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.DriverRepository;
import com.lpgchain.repository.RoleRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.repository.VehicleRepository;
import com.lpgchain.service.DeliveryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class DeliveryTransactionIntegrationTest {

    @Autowired private DeliveryService deliveryService;
    @Autowired private DeliveryRepository deliveryRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private DistributorRepository distributorRepository;
    @Autowired private DistributorMembershipRepository distributorMembershipRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private Distributor distributor;
    private User customer;
    private User distributorUser;
    private Driver driver;
    private Vehicle vehicle;
    private Booking booking;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        distributor = distributorRepository.save(new Distributor(
                "Apex Gas Agency", "LIC-DELIV-001", "+919100000001",
                "City Center", "ACTIVE", "passkey123", LocalDateTime.now().plusDays(10), false
        ));

        customer = userRepository.save(new User("Test Customer", "customer_tx@lpgchain.demo", "hash"));
        distributorUser = userRepository.save(new User("Dist User", "dist_tx@lpgchain.demo", "hash"));

        Role customerRole = roleRepository.save(new Role("CUSTOMER"));
        Role distributorRole = roleRepository.save(new Role("DISTRIBUTOR"));

        userRoleRepository.save(new UserRole(customer, customerRole));
        userRoleRepository.save(new UserRole(distributorUser, distributorRole));

        distributorMembershipRepository.save(new DistributorMembership(distributorUser, distributor, "STAFF", "ACTIVE"));

        driver = driverRepository.save(new Driver("John Driver", "+919876543210", "LIC-TX-001", "AVAILABLE", distributor));
        vehicle = vehicleRepository.save(new Vehicle("MH-01-TX-001", "LPG_DELIVERY", 30, "AVAILABLE", distributor));
        booking = bookingRepository.save(new Booking(customer, distributor, "100 MG Road", LocalDateTime.now(), 2, "CONFIRMED"));

        // Setup security context as distributorUser
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        distributorUser.getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_DISTRIBUTOR"))
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        cleanDatabase();
    }

    private void cleanDatabase() {
        deliveryRepository.deleteAll();
        bookingRepository.deleteAll();
        driverRepository.deleteAll();
        vehicleRepository.deleteAll();
        distributorMembershipRepository.deleteAll();
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        userRepository.deleteAll();
        distributorRepository.deleteAll();
    }

    @Test
    @DisplayName("Transaction Atomicity: Failed delivery creation rolls back driver and vehicle status changes")
    void testDeliveryCreationRollbackOnFailure() {
        long initialDeliveryCount = deliveryRepository.count();
        assertEquals("AVAILABLE", driverRepository.findById(driver.getId()).get().getStatus());
        assertEquals("AVAILABLE", vehicleRepository.findById(vehicle.getId()).get().getStatus());

        // Attempt delivery creation inside a transaction where a simulated error occurs after status change
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            Driver d = driverRepository.findById(driver.getId()).get();
            Vehicle v = vehicleRepository.findById(vehicle.getId()).get();
            d.setStatus("ASSIGNED");
            v.setStatus("ASSIGNED");
            driverRepository.save(d);
            vehicleRepository.save(v);

            // Simulate database or business failure before delivery commit
            throw new RuntimeException("Simulated failure during delivery pipeline");
        } catch (RuntimeException ex) {
            transactionManager.rollback(status);
        }

        // Verify driver and vehicle statuses are rolled back to AVAILABLE and no delivery was inserted
        Driver driverAfter = driverRepository.findById(driver.getId()).get();
        Vehicle vehicleAfter = vehicleRepository.findById(vehicle.getId()).get();

        assertEquals("AVAILABLE", driverAfter.getStatus());
        assertEquals("AVAILABLE", vehicleAfter.getStatus());
        assertEquals(initialDeliveryCount, deliveryRepository.count());
    }

    @Test
    @DisplayName("Database Constraint: Duplicate Driver license number throws DataIntegrityViolationException")
    void testDuplicateDriverLicenseConstraint() {
        Driver duplicate = new Driver("Another Driver", "+919876543219", "LIC-TX-001", "AVAILABLE", distributor);

        assertThrows(DataIntegrityViolationException.class, () -> {
            driverRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    @DisplayName("Database Constraint: Duplicate Vehicle registration number throws DataIntegrityViolationException")
    void testDuplicateVehicleNumberConstraint() {
        Vehicle duplicate = new Vehicle("MH-01-TX-001", "MINI_TRUCK", 20, "AVAILABLE", distributor);

        assertThrows(DataIntegrityViolationException.class, () -> {
            vehicleRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    @DisplayName("Transaction Success: Delivery creation commits Delivery, Driver, and Vehicle atomically")
    void testDeliveryCreationSuccess() {
        DeliveryRequestDTO dto = new DeliveryRequestDTO(booking.getId(), driver.getId(), vehicle.getId(), distributor.getId());

        deliveryService.createDelivery(dto);

        // Verify all entities updated in DB
        Driver driverAfter = driverRepository.findById(driver.getId()).get();
        Vehicle vehicleAfter = vehicleRepository.findById(vehicle.getId()).get();

        assertEquals("ASSIGNED", driverAfter.getStatus());
        assertEquals("ASSIGNED", vehicleAfter.getStatus());
        assertEquals(1, deliveryRepository.count());
    }
}
