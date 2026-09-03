package com.lpgchain.service;

import com.lpgchain.dto.BookingRequestDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.User;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class BookingService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "PENDING", "CONFIRMED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"
    );

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "PENDING",          Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED",        Set.of("OUT_FOR_DELIVERY", "CANCELLED"),
            "OUT_FOR_DELIVERY", Set.of("DELIVERED"),
            "DELIVERED",        Set.of(),
            "CANCELLED",        Set.of()
    );

    private final BookingRepository bookingRepository;
    private final DistributorRepository distributorRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          DistributorRepository distributorRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.distributorRepository = distributorRepository;
        this.userRepository = userRepository;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    /**
     * Creates a new booking from a validated BookingRequestDTO.
     * Resolves customer and optional distributor from their IDs.
     * Forces status = PENDING and defaults bookingDate to now().
     */
    public StatusUpdateResult createBookingFromDTO(BookingRequestDTO dto) {
        Optional<User> customerOpt = userRepository.findById(dto.getCustomerId());
        if (customerOpt.isEmpty()) {
            return StatusUpdateResult.notFound();
        }

        Booking booking = new Booking();
        booking.setCustomer(customerOpt.get());
        booking.setDeliveryAddress(dto.getDeliveryAddress());
        booking.setQuantity(dto.getQuantity());
        booking.setStatus("PENDING");
        booking.setBookingDate(dto.getBookingDate() != null ? dto.getBookingDate() : LocalDateTime.now());

        if (dto.getDistributorId() != null) {
            Optional<Distributor> distributorOpt = distributorRepository.findById(dto.getDistributorId());
            if (distributorOpt.isEmpty()) {
                return StatusUpdateResult.invalid("Distributor not found with id: " + dto.getDistributorId());
            }
            booking.setDistributor(distributorOpt.get());
        }

        return StatusUpdateResult.success(bookingRepository.save(booking));
    }

    /**
     * Creates a new booking directly from a Booking entity.
     * Always sets status to PENDING and bookingDate to now() if not provided.
     */
    public Booking createBooking(Booking booking) {
        booking.setStatus("PENDING");
        if (booking.getBookingDate() == null) {
            booking.setBookingDate(LocalDateTime.now());
        }
        return bookingRepository.save(booking);
    }

    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    public List<Booking> getBookingsByCustomerId(Long customerId) {
        return bookingRepository.findByCustomerId(customerId);
    }

    public List<Booking> getBookingsByDistributorId(Long distributorId) {
        return bookingRepository.findByDistributorId(distributorId);
    }

    /**
     * Assigns a Distributor to a Booking.
     * The booking must be in PENDING status.
     * Does not change the booking status.
     */
    public StatusUpdateResult assignDistributor(Long bookingId, Long distributorId) {
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
        return StatusUpdateResult.success(bookingRepository.save(booking));
    }

    /**
     * Updates the status of a booking.
     * Returns empty Optional if booking not found.
     * Returns error message String if status is invalid or transition is not allowed.
     * Returns updated Booking on success.
     */
    public StatusUpdateResult updateBookingStatus(Long id, String newStatus) {
        if (!VALID_STATUSES.contains(newStatus)) {
            return StatusUpdateResult.invalid("Invalid status: " + newStatus +
                    ". Allowed values: " + VALID_STATUSES);
        }

        Optional<Booking> optional = bookingRepository.findById(id);
        if (optional.isEmpty()) {
            return StatusUpdateResult.notFound();
        }

        Booking booking = optional.get();
        String currentStatus = booking.getStatus();
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!allowed.contains(newStatus)) {
            return StatusUpdateResult.invalid("Transition from " + currentStatus +
                    " to " + newStatus + " is not allowed.");
        }

        booking.setStatus(newStatus);
        return StatusUpdateResult.success(bookingRepository.save(booking));
    }

    // ─── Inner result type to communicate outcome without throwing exceptions ───

    public static class StatusUpdateResult {
        public enum Type { SUCCESS, NOT_FOUND, INVALID }

        private final Type type;
        private final Booking booking;
        private final String errorMessage;

        private StatusUpdateResult(Type type, Booking booking, String errorMessage) {
            this.type = type;
            this.booking = booking;
            this.errorMessage = errorMessage;
        }

        public static StatusUpdateResult success(Booking booking) {
            return new StatusUpdateResult(Type.SUCCESS, booking, null);
        }

        public static StatusUpdateResult notFound() {
            return new StatusUpdateResult(Type.NOT_FOUND, null, null);
        }

        public static StatusUpdateResult invalid(String message) {
            return new StatusUpdateResult(Type.INVALID, null, message);
        }

        public Type getType() { return type; }
        public Booking getBooking() { return booking; }
        public String getErrorMessage() { return errorMessage; }
    }
}
