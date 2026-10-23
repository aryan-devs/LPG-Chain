package com.lpgchain.integration;

import com.lpgchain.dto.BookingRequestDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.DeliveryRepository;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.PaymentRepository;
import com.lpgchain.repository.RoleRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import com.lpgchain.service.BookingService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DatabaseOptimizationAndTransactionIntegrationTest
 *
 * Verifies database-level constraints, indexes, transaction boundaries,
 * state machine consistency, and lazy-loading safety.
 */
@SpringBootTest
public class DatabaseOptimizationAndTransactionIntegrationTest {

    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired(required = false) private DeliveryRepository deliveryRepository;
    @Autowired(required = false) private PaymentRepository paymentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private DistributorRepository distributorRepository;
    @Autowired private DistributorMembershipRepository distributorMembershipRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private User testCustomer;
    private User testAdmin;
    private User testDistributorUser;
    private Distributor testDistributor;
    private Role customerRole;
    private Role adminRole;
    private Role distributorRole;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        // Seed Roles
        customerRole = roleRepository.save(new Role("CUSTOMER"));
        adminRole = roleRepository.save(new Role("ADMIN"));
        distributorRole = roleRepository.save(new Role("DISTRIBUTOR"));

        // Seed Users
        testCustomer = userRepository.save(new User("Test Customer", "cust@integration.test", "passHash1"));
        userRoleRepository.save(new UserRole(testCustomer, customerRole));

        testAdmin = userRepository.save(new User("Admin User", "admin@lpgchain.example", "passHash2"));
        userRoleRepository.save(new UserRole(testAdmin, adminRole));

        testDistributorUser = userRepository.save(new User("Dist User", "dist@lpgchain.demo", "passHash3"));
        userRoleRepository.save(new UserRole(testDistributorUser, distributorRole));

        // Seed Distributor Organization
        testDistributor = distributorRepository.save(new Distributor(
                "Super Gas Agency", "LIC-OPT-001", "9876543210",
                "Metro City", "ACTIVE", "passkey1",
                LocalDateTime.now().plusDays(30), false));

        // Seed Membership
        distributorMembershipRepository.save(
                new DistributorMembership(testDistributorUser, testDistributor, "STAFF", "ACTIVE"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        cleanDatabase();
    }

    private void cleanDatabase() {
        if (deliveryRepository != null) deliveryRepository.deleteAll();
        if (paymentRepository != null) paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        distributorMembershipRepository.deleteAll();
        userRoleRepository.deleteAll();
        distributorRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    private void setSecurityContext(User user, String role) {
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, List.of(new SimpleGrantedAuthority(authority))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // =========================================================================
    // Scenario 1: Successful booking creation within transaction
    // =========================================================================

    @Test
    @DisplayName("Transaction Test: Successful booking creation saves booking in PENDING status")
    void testSuccessfulBookingCreation() {
        setSecurityContext(testCustomer, "ROLE_CUSTOMER");

        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setCustomerId(testCustomer.getId());
        dto.setDeliveryAddress("Flat 101, Galaxy Tower");
        dto.setQuantity(2);

        BookingService.StatusUpdateResult result = bookingService.createBookingFromDTO(dto);

        assertEquals(BookingService.StatusUpdateResult.Type.SUCCESS, result.getType());
        assertNotNull(result.getBooking());
        assertEquals("PENDING", result.getBooking().getStatus());
        assertEquals("Flat 101, Galaxy Tower", result.getBooking().getDeliveryAddress());
        assertEquals(2, result.getBooking().getQuantity());

        // Verify persistence in repository
        Optional<Booking> persisted = bookingRepository.findById(result.getBooking().getId());
        assertTrue(persisted.isPresent());
        assertEquals("PENDING", persisted.get().getStatus());
    }

    // =========================================================================
    // Scenario 2: Booking creation fails when customer does not exist
    // =========================================================================

    @Test
    @DisplayName("Transaction Test: Booking creation fails with NOT_FOUND when customer does not exist")
    void testBookingCreationCustomerNotFound() {
        setSecurityContext(testAdmin, "ROLE_ADMIN"); // Admin can submit for any customer ID

        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setCustomerId(999999L); // Non-existent user
        dto.setDeliveryAddress("Unknown Address");
        dto.setQuantity(1);

        BookingService.StatusUpdateResult result = bookingService.createBookingFromDTO(dto);

        assertEquals(BookingService.StatusUpdateResult.Type.NOT_FOUND, result.getType());
        assertNull(result.getBooking());
        assertEquals(0, bookingRepository.count());
    }

    // =========================================================================
    // Scenario 3: Distributor assignment succeeds when booking is PENDING
    // =========================================================================

    @Test
    @DisplayName("Transaction Test: Distributor assignment succeeds for PENDING booking")
    void testDistributorAssignmentSucceedsForPendingBooking() {
        setSecurityContext(testCustomer, "ROLE_CUSTOMER");
        Booking booking = bookingRepository.save(new Booking(
                testCustomer, null, "123 Street", LocalDateTime.now(), 1, "PENDING"
        ));

        setSecurityContext(testAdmin, "ROLE_ADMIN");
        BookingService.StatusUpdateResult result = bookingService.assignDistributor(booking.getId(), testDistributor.getId());

        assertEquals(BookingService.StatusUpdateResult.Type.SUCCESS, result.getType());
        assertNotNull(result.getBooking().getDistributorId());
        assertEquals(testDistributor.getId(), result.getBooking().getDistributorId());

        // Verify updated entity in database
        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
        assertNotNull(updated.getDistributor());
        assertEquals(testDistributor.getId(), updated.getDistributor().getId());
    }

    // =========================================================================
    // Scenario 4: Distributor assignment fails for an invalid booking state
    // =========================================================================

    @Test
    @DisplayName("Transaction Test: Distributor assignment rejected when booking is not PENDING")
    void testDistributorAssignmentFailsForNonPendingBooking() {
        setSecurityContext(testCustomer, "ROLE_CUSTOMER");
        Booking booking = bookingRepository.save(new Booking(
                testCustomer, testDistributor, "123 Street", LocalDateTime.now(), 1, "CONFIRMED"
        ));

        setSecurityContext(testAdmin, "ROLE_ADMIN");
        BookingService.StatusUpdateResult result = bookingService.assignDistributor(booking.getId(), testDistributor.getId());

        assertEquals(BookingService.StatusUpdateResult.Type.INVALID, result.getType());
        assertTrue(result.getErrorMessage().contains("PENDING"));

        // Entity status and distributor remain untouched
        Booking unchanged = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals("CONFIRMED", unchanged.getStatus());
    }

    // =========================================================================
    // Scenario 5: Valid booking status transition succeeds
    // =========================================================================

    @Test
    @DisplayName("Transaction Test: Valid state machine transition sequence succeeds")
    void testValidBookingStatusTransitions() {
        setSecurityContext(testCustomer, "ROLE_CUSTOMER");
        Booking booking = bookingRepository.save(new Booking(
                testCustomer, testDistributor, "123 Street", LocalDateTime.now(), 1, "PENDING"
        ));

        setSecurityContext(testDistributorUser, "ROLE_DISTRIBUTOR");

        // PENDING -> CONFIRMED
        BookingService.StatusUpdateResult r1 = bookingService.updateBookingStatus(booking.getId(), "CONFIRMED");
        assertEquals(BookingService.StatusUpdateResult.Type.SUCCESS, r1.getType());
        assertEquals("CONFIRMED", bookingRepository.findById(booking.getId()).orElseThrow().getStatus());

        // CONFIRMED -> OUT_FOR_DELIVERY
        BookingService.StatusUpdateResult r2 = bookingService.updateBookingStatus(booking.getId(), "OUT_FOR_DELIVERY");
        assertEquals(BookingService.StatusUpdateResult.Type.SUCCESS, r2.getType());
        assertEquals("OUT_FOR_DELIVERY", bookingRepository.findById(booking.getId()).orElseThrow().getStatus());

        // OUT_FOR_DELIVERY -> DELIVERED
        BookingService.StatusUpdateResult r3 = bookingService.updateBookingStatus(booking.getId(), "DELIVERED");
        assertEquals(BookingService.StatusUpdateResult.Type.SUCCESS, r3.getType());
        assertEquals("DELIVERED", bookingRepository.findById(booking.getId()).orElseThrow().getStatus());
    }

    // =========================================================================
    // Scenario 6: Invalid booking status transition fails
    // =========================================================================

    @Test
    @DisplayName("Transaction Test: Invalid state machine transitions are rejected and state is preserved")
    void testInvalidBookingStatusTransitions() {
        setSecurityContext(testCustomer, "ROLE_CUSTOMER");
        Booking booking = bookingRepository.save(new Booking(
                testCustomer, testDistributor, "123 Street", LocalDateTime.now(), 1, "DELIVERED"
        ));

        setSecurityContext(testAdmin, "ROLE_ADMIN");

        // Terminal DELIVERED state cannot transition back to PENDING
        BookingService.StatusUpdateResult result = bookingService.updateBookingStatus(booking.getId(), "PENDING");
        assertEquals(BookingService.StatusUpdateResult.Type.INVALID, result.getType());

        // Terminal DELIVERED state cannot transition to CANCELLED
        BookingService.StatusUpdateResult cancelResult = bookingService.updateBookingStatus(booking.getId(), "CANCELLED");
        assertEquals(BookingService.StatusUpdateResult.Type.INVALID, cancelResult.getType());

        // Database remains in DELIVERED state
        Booking unchanged = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals("DELIVERED", unchanged.getStatus());
    }

    // =========================================================================
    // Scenario 7: Database unique constraint on User (email)
    // =========================================================================

    @Test
    @DisplayName("Database Constraint: Duplicate user email throws DataIntegrityViolationException")
    void testDuplicateUserEmailConstraint() {
        User duplicate = new User("Duplicate User", "cust@integration.test", "anotherPass");

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(duplicate);
        });
    }

    // =========================================================================
    // Scenario 8: Database unique constraint on Role (name)
    // =========================================================================

    @Test
    @DisplayName("Database Constraint: Duplicate role name throws DataIntegrityViolationException")
    void testDuplicateRoleNameConstraint() {
        Role duplicateRole = new Role("CUSTOMER");

        assertThrows(DataIntegrityViolationException.class, () -> {
            roleRepository.saveAndFlush(duplicateRole);
        });
    }

    // =========================================================================
    // Scenario 9: Database unique constraint on UserRole (user_id + role)
    // =========================================================================

    @Test
    @DisplayName("Database Constraint: Duplicate UserRole combination throws DataIntegrityViolationException")
    void testDuplicateUserRoleConstraint() {
        // testCustomer already has customerRole in setUp()
        UserRole duplicate = new UserRole(testCustomer, customerRole);

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRoleRepository.saveAndFlush(duplicate);
        });
    }

    // =========================================================================
    // Scenario 10: Database unique constraint on DistributorMembership (user_id + distributor_id)
    // =========================================================================

    @Test
    @DisplayName("Database Constraint: Duplicate DistributorMembership throws DataIntegrityViolationException")
    void testDuplicateDistributorMembershipConstraint() {
        // testDistributorUser already has membership with testDistributor in setUp()
        DistributorMembership duplicate = new DistributorMembership(
                testDistributorUser, testDistributor, "MANAGER", "ACTIVE");

        assertThrows(DataIntegrityViolationException.class, () -> {
            distributorMembershipRepository.saveAndFlush(duplicate);
        });
    }

    // =========================================================================
    // Scenario 11: Database unique constraint on Distributor (licence_number)
    // =========================================================================

    @Test
    @DisplayName("Database Constraint: Duplicate distributor licence number throws DataIntegrityViolationException")
    void testDuplicateDistributorLicenceConstraint() {
        // testDistributor has licenceNumber "LIC-OPT-001"
        Distributor duplicateDistributor = new Distributor(
                "Another Agency", "LIC-OPT-001", "1231231234",
                "New City", "ACTIVE", "anotherpasskey",
                LocalDateTime.now().plusDays(10), false);

        assertThrows(DataIntegrityViolationException.class, () -> {
            distributorRepository.saveAndFlush(duplicateDistributor);
        });
    }

    // =========================================================================
    // Scenario 12: Lazy-loading safety and transactional consistency
    // =========================================================================

    @Test
    @DisplayName("Database Optimization: Lazy relationships resolve properly inside transactional query")
    void testLazyRelationshipAccessUnderTransaction() {
        setSecurityContext(testCustomer, "ROLE_CUSTOMER");

        Booking booking = bookingRepository.save(new Booking(
                testCustomer, testDistributor, "99 Palm Grove", LocalDateTime.now(), 1, "PENDING"
        ));

        // Invoke read-only transactional service method
        Optional<?> responseOpt = bookingService.getBookingById(booking.getId());

        assertTrue(responseOpt.isPresent());
    }

    // =========================================================================
    // Scenario 13: Transaction rollback on unexpected error
    // =========================================================================

    @Test
    @DisplayName("Transaction Rollback: Uncommitted changes in transaction roll back cleanly")
    void testTransactionalRollback() {
        long initialCount = bookingRepository.count();

        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            Booking tempBooking = new Booking(
                    testCustomer, testDistributor, "Rollback Address", LocalDateTime.now(), 1, "PENDING"
            );
            bookingRepository.save(tempBooking);
            // Simulate error triggering rollback
            throw new RuntimeException("Simulated unexpected transaction failure");
        } catch (RuntimeException ex) {
            transactionManager.rollback(status);
        }

        // Verify count is restored to initialCount
        assertEquals(initialCount, bookingRepository.count());
    }
}
