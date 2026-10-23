package com.lpgchain.service;

import com.lpgchain.dto.BookingRequestDTO;
import com.lpgchain.dto.BookingResponseDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.entity.User;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BookingService {

    public static final Set<String> VALID_STATUSES = Set.of(
            "PENDING", "CONFIRMED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"
    );

    public static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "PENDING",          Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED",        Set.of("OUT_FOR_DELIVERY", "CANCELLED"),
            "OUT_FOR_DELIVERY", Set.of("DELIVERED"),
            "DELIVERED",        Set.of(),
            "CANCELLED",        Set.of()
    );

    private final BookingRepository bookingRepository;
    private final DistributorRepository distributorRepository;
    private final UserRepository userRepository;
    private final EntityMapper entityMapper;
    private final BookingAuthorizationService bookingAuthorizationService;
    private final DistributorMembershipRepository distributorMembershipRepository;
    private final com.lpgchain.repository.PaymentRepository paymentRepository;
    private final com.lpgchain.repository.SavedAddressRepository savedAddressRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public BookingService(BookingRepository bookingRepository,
                          DistributorRepository distributorRepository,
                          UserRepository userRepository,
                          EntityMapper entityMapper,
                          BookingAuthorizationService bookingAuthorizationService,
                          DistributorMembershipRepository distributorMembershipRepository,
                          com.lpgchain.repository.PaymentRepository paymentRepository,
                          com.lpgchain.repository.SavedAddressRepository savedAddressRepository,
                          RealtimeEventPublisher realtimeEventPublisher) {
        this.bookingRepository = bookingRepository;
        this.distributorRepository = distributorRepository;
        this.userRepository = userRepository;
        this.entityMapper = entityMapper;
        this.bookingAuthorizationService = bookingAuthorizationService;
        this.distributorMembershipRepository = distributorMembershipRepository;
        this.paymentRepository = paymentRepository;
        this.savedAddressRepository = savedAddressRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    public List<BookingResponseDTO> getAllBookings() {
        Authentication auth = bookingAuthorizationService.getAuthentication();
        if (bookingAuthorizationService.isAdmin(auth)) {
            return bookingRepository.findAll().stream()
                    .map(entityMapper::toBookingResponseDTO)
                    .toList();
        }

        User currentUser = bookingAuthorizationService.getAuthenticatedUser();

        if (bookingAuthorizationService.isCustomer(auth)) {
            return bookingRepository.findByCustomerId(currentUser.getId()).stream()
                    .map(entityMapper::toBookingResponseDTO)
                    .toList();
        }

        if (bookingAuthorizationService.isDistributor(auth)) {
            List<DistributorMembership> memberships = distributorMembershipRepository.findByUserId(currentUser.getId());
            return memberships.stream()
                    .map(DistributorMembership::getDistributor)
                    .filter(Objects::nonNull)
                    .map(Distributor::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .flatMap(distId -> bookingRepository.findByDistributorId(distId).stream())
                    .map(entityMapper::toBookingResponseDTO)
                    .toList();
        }

        return List.of();
    }

    public Optional<BookingResponseDTO> getBookingById(Long id) {
        Optional<Booking> optional = bookingRepository.findById(id);
        if (optional.isEmpty()) {
            return Optional.empty();
        }

        Booking booking = optional.get();
        // ReBAC authorization check: Customer must own, Distributor must be assigned, Admin allowed
        bookingAuthorizationService.checkCanAccessBooking(booking);

        return Optional.of(entityMapper.toBookingResponseDTO(booking));
    }

    /**
     * Creates a new booking from a validated BookingRequestDTO.
     * Enforces that customers create bookings only for their authenticated user ID.
     */
    @Transactional
    public StatusUpdateResult createBookingFromDTO(BookingRequestDTO dto) {
        bookingAuthorizationService.checkCanCreateBooking(dto);

        Optional<User> customerOpt = userRepository.findById(dto.getCustomerId());
        if (customerOpt.isEmpty()) {
            return StatusUpdateResult.notFound();
        }

        String deliveryAddress = dto.getDeliveryAddress();
        if (dto.getSavedAddressId() != null) {
            Optional<com.lpgchain.entity.SavedAddress> addrOpt = savedAddressRepository.findByIdAndUserId(dto.getSavedAddressId(), customerOpt.get().getId());
            if (addrOpt.isPresent()) {
                com.lpgchain.entity.SavedAddress a = addrOpt.get();
                deliveryAddress = a.getAddressLine1() +
                        (a.getAddressLine2() != null ? ", " + a.getAddressLine2() : "") +
                        (a.getLandmark() != null ? ", Near " + a.getLandmark() : "") +
                        ", " + a.getCity() + ", " + a.getState() + " - " + a.getPincode();
            }
        }

        Booking booking = new Booking();
        booking.setCustomer(customerOpt.get());
        booking.setDeliveryAddress(deliveryAddress);
        booking.setQuantity(dto.getQuantity());
        booking.setStatus("PENDING");
        booking.setBookingDate(dto.getBookingDate() != null ? dto.getBookingDate() : LocalDateTime.now());

        com.lpgchain.entity.CylinderCategory cat = dto.getCylinderCategory() != null
                ? com.lpgchain.entity.CylinderCategory.fromCode(dto.getCylinderCategory())
                : com.lpgchain.entity.CylinderCategory.DOMESTIC_14_2;
        booking.setCylinderCategory(cat.getCode());
        booking.setCapacityKg(dto.getCapacityKg() != null ? dto.getCapacityKg() : cat.getCapacityKg());

        // Trusted server-side price calculation
        double unitPrice;
        double subsidyPerUnit = 0.0;
        if (cat == com.lpgchain.entity.CylinderCategory.COMMERCIAL_19_0) {
            unitPrice = 1785.0;
        } else if (cat == com.lpgchain.entity.CylinderCategory.DOMESTIC_5_0) {
            unitPrice = 360.0;
        } else {
            unitPrice = 853.0;
            subsidyPerUnit = 200.0;
        }

        double totalAmount = unitPrice * dto.getQuantity();
        double subsidyAmount = subsidyPerUnit * dto.getQuantity();
        double netAmount = totalAmount - subsidyAmount;

        booking.setUnitPrice(unitPrice);
        booking.setTotalAmount(totalAmount);
        booking.setSubsidyAmount(subsidyAmount);
        booking.setNetAmount(netAmount);
        booking.setBookingNumber("BK-" + System.currentTimeMillis() + "-" + (int)(1000 + Math.random() * 9000));

        String pMethod = (dto.getPaymentMethod() != null && "CASH_ON_DELIVERY".equalsIgnoreCase(dto.getPaymentMethod().trim()))
                ? "CASH_ON_DELIVERY"
                : "DEMO_QR";
        booking.setPaymentMethod(pMethod);
        booking.setPaymentStatus("PENDING");

        String txnRef = "TXN-DEMO-" + java.util.UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        booking.setPaymentReference(txnRef);

        if (dto.getDistributorId() != null) {
            Optional<Distributor> distributorOpt = distributorRepository.findById(dto.getDistributorId());
            if (distributorOpt.isEmpty()) {
                return StatusUpdateResult.invalid("Distributor not found with id: " + dto.getDistributorId());
            }
            booking.setDistributor(distributorOpt.get());
        }

        Booking savedBooking = bookingRepository.save(booking);

        // Persist initial payment record in MySQL
        com.lpgchain.entity.Payment payment = new com.lpgchain.entity.Payment();
        payment.setBooking(savedBooking);
        payment.setCustomer(savedBooking.getCustomer());
        payment.setAmount(totalAmount);
        payment.setSubsidyAmount(subsidyAmount);
        payment.setNetAmount(netAmount);
        payment.setPaymentMethod(pMethod);
        payment.setPaymentStatus("PENDING");
        payment.setTransactionReference(txnRef);
        payment.setIsSimulated("DEMO_QR".equals(pMethod));
        paymentRepository.save(payment);

        BookingResponseDTO resDTO = entityMapper.toBookingResponseDTO(savedBooking);

        // Real-time notification publishing
        realtimeEventPublisher.publishCustomerEvent(customerOpt.get().getId(), "BOOKING_CREATED", resDTO);
        if (savedBooking.getDistributor() != null) {
            realtimeEventPublisher.publishDistributorEvent(savedBooking.getDistributor().getId(), "BOOKING_CREATED", resDTO);
        }
        realtimeEventPublisher.publishAdminEvent("BOOKING_CREATED", resDTO);

        return StatusUpdateResult.success(resDTO);
    }

    /**
     * Creates a new booking directly from a Booking entity.
     * Always sets status to PENDING and bookingDate to now() if not provided.
     */
    @Transactional
    public Booking createBooking(Booking booking) {
        booking.setStatus("PENDING");
        if (booking.getBookingDate() == null) {
            booking.setBookingDate(LocalDateTime.now());
        }
        return bookingRepository.save(booking);
    }

    @Transactional
    public void deleteBooking(Long id) {
        bookingAuthorizationService.checkCanDeleteBooking();
        bookingRepository.deleteById(id);
    }

    public List<BookingResponseDTO> getBookingsByCustomerId(Long customerId) {
        bookingAuthorizationService.checkCanAccessCustomerBookings(customerId);
        return bookingRepository.findByCustomerId(customerId).stream()
                .map(entityMapper::toBookingResponseDTO)
                .toList();
    }

    public List<BookingResponseDTO> getBookingsByDistributorId(Long distributorId) {
        bookingAuthorizationService.checkCanAccessDistributorBookings(distributorId);
        return bookingRepository.findByDistributorId(distributorId).stream()
                .map(entityMapper::toBookingResponseDTO)
                .toList();
    }

    /**
     * Assigns a Distributor to a Booking.
     * Restricted to administrators.
     * The booking must be in PENDING status.
     */
    @Transactional
    public StatusUpdateResult assignDistributor(Long bookingId, Long distributorId) {
        bookingAuthorizationService.checkCanAssignDistributor();

        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return StatusUpdateResult.notFound();
        }

        Booking booking = bookingOpt.get();

        if (!"PENDING".equals(booking.getStatus())) {
            return StatusUpdateResult.invalid(
                    "Distributor can only be assigned to a PENDING booking. " +
                    "Current status: " + booking.getStatus());
        }

        Optional<Distributor> distributorOpt = distributorRepository.findById(distributorId);
        if (distributorOpt.isEmpty()) {
            return StatusUpdateResult.notFound();
        }

        booking.setDistributor(distributorOpt.get());
        Booking savedBooking = bookingRepository.save(booking);
        return StatusUpdateResult.success(entityMapper.toBookingResponseDTO(savedBooking));
    }

    /**
     * Updates the status of a booking.
     * Enforces ReBAC (ownership / assignment) and ABAC (action validity & state machine transitions).
     */
    @Transactional
    public StatusUpdateResult updateBookingStatus(Long id, String newStatus) {
        return updateBookingStatus(id, newStatus, null);
    }

    /**
     * Updates the status of a booking with optional operational/rejection reason.
     * Uses pessimistic write locking to prevent concurrent race conditions.
     */
    @Transactional
    public StatusUpdateResult updateBookingStatus(Long id, String newStatus, String reason) {
        Optional<Booking> optional = bookingRepository.findByIdWithLock(id);
        if (optional.isEmpty()) {
            optional = bookingRepository.findById(id);
        }
        if (optional.isEmpty()) {
            return StatusUpdateResult.notFound();
        }

        Booking booking = optional.get();

        // 1. ReBAC + ABAC authorization check (throws AccessDeniedException for 403 Forbidden)
        bookingAuthorizationService.checkCanUpdateBookingStatus(booking, newStatus);

        // 2. Business condition validation (returns invalid for 400 Bad Request)
        if (!VALID_STATUSES.contains(newStatus)) {
            return StatusUpdateResult.invalid("Invalid status: " + newStatus +
                    ". Allowed values: " + VALID_STATUSES);
        }

        String currentStatus = booking.getStatus();
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!allowed.contains(newStatus)) {
            if (currentStatus != null && currentStatus.equalsIgnoreCase(newStatus)) {
                return StatusUpdateResult.invalid("Booking #" + id + " is already in " + currentStatus + " status.");
            }
            return StatusUpdateResult.invalid("Transition from " + currentStatus +
                    " to " + newStatus + " is not allowed.");
        }

        booking.setStatus(newStatus);
        Booking savedBooking = bookingRepository.save(booking);

        BookingResponseDTO resDTO = entityMapper.toBookingResponseDTO(savedBooking);
        if (savedBooking.getCustomer() != null) {
            realtimeEventPublisher.publishCustomerEvent(savedBooking.getCustomer().getId(), "BOOKING_STATUS_CHANGED", resDTO);
        }
        if (savedBooking.getDistributor() != null) {
            realtimeEventPublisher.publishDistributorEvent(savedBooking.getDistributor().getId(), "BOOKING_STATUS_CHANGED", resDTO);
        }
        realtimeEventPublisher.publishAdminEvent("BOOKING_STATUS_CHANGED", resDTO);

        return StatusUpdateResult.success(resDTO);
    }


    // ─── Inner result type to communicate outcome without throwing exceptions ───

    public static class StatusUpdateResult {
        public enum Type { SUCCESS, NOT_FOUND, INVALID }

        private final Type type;
        private final BookingResponseDTO booking;
        private final String errorMessage;

        private StatusUpdateResult(Type type, BookingResponseDTO booking, String errorMessage) {
            this.type = type;
            this.booking = booking;
            this.errorMessage = errorMessage;
        }

        public static StatusUpdateResult success(BookingResponseDTO booking) {
            return new StatusUpdateResult(Type.SUCCESS, booking, null);
        }

        public static StatusUpdateResult notFound() {
            return new StatusUpdateResult(Type.NOT_FOUND, null, null);
        }

        public static StatusUpdateResult invalid(String message) {
            return new StatusUpdateResult(Type.INVALID, null, message);
        }

        public Type getType() { return type; }
        public BookingResponseDTO getBooking() { return booking; }
        public String getErrorMessage() { return errorMessage; }
    }
}
