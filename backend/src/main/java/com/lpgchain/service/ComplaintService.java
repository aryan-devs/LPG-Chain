package com.lpgchain.service;

import com.lpgchain.dto.ComplaintRequestDTO;
import com.lpgchain.dto.ComplaintResponseDTO;
import com.lpgchain.dto.ComplaintStatusUpdateDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Complaint;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.User;
import com.lpgchain.exception.ResourceNotFoundException;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.BookingRepository;
import com.lpgchain.repository.ComplaintRepository;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final BookingRepository bookingRepository;
    private final DistributorRepository distributorRepository;
    private final UserRepository userRepository;
    private final DistributorMembershipRepository distributorMembershipRepository;
    private final EntityMapper entityMapper;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public ComplaintService(ComplaintRepository complaintRepository,
                            BookingRepository bookingRepository,
                            DistributorRepository distributorRepository,
                            UserRepository userRepository,
                            DistributorMembershipRepository distributorMembershipRepository,
                            EntityMapper entityMapper,
                            RealtimeEventPublisher realtimeEventPublisher) {
        this.complaintRepository = complaintRepository;
        this.bookingRepository = bookingRepository;
        this.distributorRepository = distributorRepository;
        this.userRepository = userRepository;
        this.distributorMembershipRepository = distributorMembershipRepository;
        this.entityMapper = entityMapper;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Unauthorized: Authentication required");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    private boolean hasRole(String roleName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_" + roleName) || a.getAuthority().equalsIgnoreCase(roleName));
    }

    public ComplaintResponseDTO createComplaint(ComplaintRequestDTO request) {
        User customer = getAuthenticatedUser();

        Complaint complaint = new Complaint();
        complaint.setCustomer(customer);
        complaint.setCategory(request.getCategory().trim().toUpperCase());
        complaint.setSubject(request.getSubject().trim());
        complaint.setDescription(request.getDescription().trim());
        complaint.setPriority(request.getPriority() != null ? request.getPriority().trim().toUpperCase() : "MEDIUM");
        complaint.setStatus("OPEN");
        complaint.setComplaintNumber("CMP-" + System.currentTimeMillis() + "-" + (int)(1000 + Math.random() * 9000));

        if (request.getBookingId() != null) {
            Booking booking = bookingRepository.findByIdWithDetails(request.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + request.getBookingId()));

            // Verify customer owns the booking unless admin
            if (!hasRole("ADMIN") && !booking.getCustomer().getId().equals(customer.getId())) {
                throw new AccessDeniedException("Forbidden: Cannot file complaint for another customer's booking");
            }
            complaint.setBooking(booking);
            if (booking.getDistributor() != null && complaint.getDistributor() == null) {
                complaint.setDistributor(booking.getDistributor());
            }
        }

        if (request.getDistributorId() != null && complaint.getDistributor() == null) {
            Distributor distributor = distributorRepository.findById(request.getDistributorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + request.getDistributorId()));
            complaint.setDistributor(distributor);
        }

        Complaint saved = complaintRepository.save(complaint);
        ComplaintResponseDTO dto = entityMapper.toComplaintResponseDTO(saved);

        // Real-time broadcast
        realtimeEventPublisher.publishCustomerEvent(customer.getId(), "COMPLAINT_CREATED", dto);
        if (saved.getDistributor() != null) {
            realtimeEventPublisher.publishDistributorEvent(saved.getDistributor().getId(), "COMPLAINT_CREATED", dto);
        }
        realtimeEventPublisher.publishAdminEvent("COMPLAINT_CREATED", dto);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponseDTO> getMyComplaints() {
        User customer = getAuthenticatedUser();
        return complaintRepository.findByCustomerIdWithDetails(customer.getId()).stream()
                .map(entityMapper::toComplaintResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponseDTO> getDistributorComplaints(Long distributorId) {
        User currentUser = getAuthenticatedUser();
        if (!hasRole("ADMIN")) {
            boolean hasMembership = distributorMembershipRepository.existsByUserIdAndDistributorIdAndStatusIgnoreCase(
                    currentUser.getId(), distributorId, "ACTIVE");
            if (!hasMembership) {
                throw new AccessDeniedException("Forbidden: Not an authorized member of distributor agency: " + distributorId);
            }
        }

        return complaintRepository.findByDistributorIdWithDetails(distributorId).stream()
                .map(entityMapper::toComplaintResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponseDTO> getAllComplaints(String status) {
        if (!hasRole("ADMIN")) {
            throw new AccessDeniedException("Forbidden: Administrator access required");
        }

        List<Complaint> list;
        if (status != null && !status.trim().isEmpty()) {
            list = complaintRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase());
        } else {
            list = complaintRepository.findAllWithDetails();
        }

        return list.stream()
                .map(entityMapper::toComplaintResponseDTO)
                .toList();
    }

    public ComplaintResponseDTO updateComplaintStatus(Long id, ComplaintStatusUpdateDTO dto) {
        User reviewer = getAuthenticatedUser();
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));

        // ReBAC check: Only Admin or the assigned distributor can resolve
        if (!hasRole("ADMIN")) {
            if (hasRole("DISTRIBUTOR")) {
                if (complaint.getDistributor() == null || !distributorMembershipRepository.existsByUserIdAndDistributorIdAndStatusIgnoreCase(
                        reviewer.getId(), complaint.getDistributor().getId(), "ACTIVE")) {
                    throw new AccessDeniedException("Forbidden: Not authorized to resolve complaints outside your agency");
                }
            } else {
                throw new AccessDeniedException("Forbidden: Customer cannot resolve complaints");
            }
        }

        String newStatus = dto.getStatus().trim().toUpperCase();
        complaint.setStatus(newStatus);
        if (dto.getResolutionNote() != null) {
            complaint.setResolutionNote(dto.getResolutionNote().trim());
        }
        complaint.setResolvedBy(reviewer);

        if ("RESOLVED".equals(newStatus) || "REJECTED".equals(newStatus)) {
            complaint.setResolvedAt(LocalDateTime.now());
        }

        Complaint saved = complaintRepository.save(complaint);
        ComplaintResponseDTO resDTO = entityMapper.toComplaintResponseDTO(saved);

        // Real-time broadcast
        realtimeEventPublisher.publishCustomerEvent(saved.getCustomer().getId(), "COMPLAINT_UPDATED", resDTO);
        if (saved.getDistributor() != null) {
            realtimeEventPublisher.publishDistributorEvent(saved.getDistributor().getId(), "COMPLAINT_UPDATED", resDTO);
        }
        realtimeEventPublisher.publishAdminEvent("COMPLAINT_UPDATED", resDTO);

        return resDTO;
    }
}
