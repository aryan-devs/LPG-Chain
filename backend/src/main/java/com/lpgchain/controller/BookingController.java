package com.lpgchain.controller;

import com.lpgchain.dto.BookingRequestDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.service.BookingService;
import com.lpgchain.service.BookingService.StatusUpdateResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable Long id) {
        return bookingService.getBookingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Booking>> getBookingsByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(bookingService.getBookingsByCustomerId(customerId));
    }

    @GetMapping("/distributor/{distributorId}")
    public ResponseEntity<List<Booking>> getBookingsByDistributor(@PathVariable Long distributorId) {
        return ResponseEntity.ok(bookingService.getBookingsByDistributorId(distributorId));
    }

    @PostMapping
    public ResponseEntity<?> createBooking(@Valid @RequestBody BookingRequestDTO bookingRequest) {
        StatusUpdateResult result = bookingService.createBookingFromDTO(bookingRequest);

        return switch (result.getType()) {
            case SUCCESS   -> ResponseEntity.status(HttpStatus.CREATED).body(result.getBooking());
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Customer not found with id: " + bookingRequest.getCustomerId());
            case INVALID   -> ResponseEntity.badRequest().body(result.getErrorMessage());
        };
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateBookingStatus(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body("Field 'status' is required.");
        }

        StatusUpdateResult result = bookingService.updateBookingStatus(id, newStatus.toUpperCase());

        return switch (result.getType()) {
            case SUCCESS   -> ResponseEntity.ok(result.getBooking());
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case INVALID   -> ResponseEntity.badRequest().body(result.getErrorMessage());
        };
    }

    @PatchMapping("/{bookingId}/assign-distributor/{distributorId}")
    public ResponseEntity<?> assignDistributor(@PathVariable Long bookingId,
                                               @PathVariable Long distributorId) {
        StatusUpdateResult result = bookingService.assignDistributor(bookingId, distributorId);

        return switch (result.getType()) {
            case SUCCESS   -> ResponseEntity.ok(result.getBooking());
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case INVALID   -> ResponseEntity.badRequest().body(result.getErrorMessage());
        };
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
}
