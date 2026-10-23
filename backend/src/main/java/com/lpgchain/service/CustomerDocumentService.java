package com.lpgchain.service;

import com.lpgchain.dto.CustomerDocumentDTO;
import com.lpgchain.dto.DocumentReviewDTO;
import com.lpgchain.dto.KycSummaryDTO;
import com.lpgchain.entity.CustomerDocument;
import com.lpgchain.entity.User;
import com.lpgchain.exception.ResourceNotFoundException;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.CustomerDocumentRepository;
import com.lpgchain.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class CustomerDocumentService {

    public static final Set<String> REQUIRED_DOC_TYPES = Set.of(
            "AADHAAR_CARD", "RATION_CARD", "ELECTRICITY_BILL", "BANK_PASSBOOK"
    );

    private static final Path UPLOAD_ROOT = Paths.get("uploads", "documents").toAbsolutePath().normalize();

    private final CustomerDocumentRepository customerDocumentRepository;
    private final UserRepository userRepository;
    private final EntityMapper entityMapper;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public CustomerDocumentService(CustomerDocumentRepository customerDocumentRepository,
                                   UserRepository userRepository,
                                   EntityMapper entityMapper,
                                   RealtimeEventPublisher realtimeEventPublisher) {
        this.customerDocumentRepository = customerDocumentRepository;
        this.userRepository = userRepository;
        this.entityMapper = entityMapper;
        this.realtimeEventPublisher = realtimeEventPublisher;

        try {
            Files.createDirectories(UPLOAD_ROOT);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize document upload root directory", e);
        }
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

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".jpg", ".jpeg", ".png", ".webp");
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/webp", "image/pjpeg"
    );
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    public CustomerDocumentDTO uploadDocument(MultipartFile file, String documentType, String documentNumber) {
        User customer = getAuthenticatedUser();

        if (documentType == null || documentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Document category is required");
        }

        String normalizedType = documentType.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        if (!REQUIRED_DOC_TYPES.contains(normalizedType) && !"PASSPORT_PHOTO".equals(normalizedType)) {
            throw new IllegalArgumentException("Invalid document category: " + documentType + ". Supported: " + REQUIRED_DOC_TYPES);
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty. Please select a valid document.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of 10MB (Uploaded: " + (file.getSize() / (1024 * 1024)) + "MB).");
        }

        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type (" + contentType + "). Only PDF and Images (JPG, PNG, WEBP) are accepted.");
        }

        // Prevent path traversal and sanitize filename
        String originalFilename = file.getOriginalFilename() != null ? Paths.get(file.getOriginalFilename()).getFileName().toString() : "doc.pdf";
        String extension = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            extension = originalFilename.substring(i).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file extension: '" + extension + "'. Allowed extensions: " + ALLOWED_EXTENSIONS);
        }

        String safeStoredFilename = normalizedType + "_" + System.currentTimeMillis() + extension;
        Path customerDir = UPLOAD_ROOT.resolve(String.valueOf(customer.getId())).normalize();
        Path destination = customerDir.resolve(safeStoredFilename).normalize();

        try {
            Files.createDirectories(customerDir);

            // Verify path traversal prevention
            if (!destination.startsWith(customerDir)) {
                throw new SecurityException("Invalid path traversal attempt");
            }

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write document to storage: " + e.getMessage(), e);
        }

        try {
            // Find existing document or create new, avoiding NonUniqueResultException
            List<CustomerDocument> existingDocs = customerDocumentRepository
                    .findByCustomerIdAndDocumentTypeOrderByCreatedAtDesc(customer.getId(), normalizedType);
            CustomerDocument doc = existingDocs.isEmpty() ? new CustomerDocument() : existingDocs.get(0);

            if (doc.getId() == null) {
                doc.setCustomer(customer);
                doc.setDocumentType(normalizedType);
            }

            doc.setDocumentNumber(documentNumber != null && !documentNumber.trim().isEmpty() ? documentNumber.trim() : null);
            doc.setFileName(originalFilename);
            doc.setFilePath(destination.toString());
            doc.setContentType(contentType != null ? contentType : "application/octet-stream");
            doc.setFileSizeBytes(file.getSize());
            doc.setStatus("SUBMITTED");
            doc.setRejectionReason(null);
            doc.setReviewedBy(null);
            doc.setReviewedAt(null);

            CustomerDocument saved = customerDocumentRepository.save(doc);
            CustomerDocumentDTO dto = entityMapper.toCustomerDocumentDTO(saved);

            // Real-time broadcast
            realtimeEventPublisher.publishCustomerEvent(customer.getId(), "KYC_DOCUMENT_SUBMITTED", dto);
            realtimeEventPublisher.publishAdminEvent("KYC_DOCUMENT_SUBMITTED", dto);

            return dto;
        } catch (Exception ex) {
            // Roll back the copied file on database failure to prevent orphan files
            try {
                Files.deleteIfExists(destination);
            } catch (IOException ignored) {}
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public KycSummaryDTO getKycSummary() {
        User customer = getAuthenticatedUser();
        List<CustomerDocument> existingDocs = customerDocumentRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());

        int totalRequired = REQUIRED_DOC_TYPES.size();
        int approvedCount = 0;
        int rejectedCount = 0;
        int pendingCount = 0;
        int submittedCount = existingDocs.size();

        for (CustomerDocument doc : existingDocs) {
            if ("APPROVED".equalsIgnoreCase(doc.getStatus())) {
                approvedCount++;
            } else if ("REJECTED".equalsIgnoreCase(doc.getStatus())) {
                rejectedCount++;
            } else if ("SUBMITTED".equalsIgnoreCase(doc.getStatus())) {
                pendingCount++;
            }
        }

        int remainingRequiredCount = Math.max(0, totalRequired - approvedCount);
        boolean allApproved = (approvedCount >= totalRequired);

        String statusMessage;
        if (allApproved) {
            statusMessage = "Your documents are up to date.";
        } else if (pendingCount > 0 && remainingRequiredCount == pendingCount) {
            statusMessage = "All required documents are submitted and awaiting review.";
        } else {
            statusMessage = "Please submit the remaining required documents for KYC compliance.";
        }

        KycSummaryDTO summary = new KycSummaryDTO();
        summary.setTotalRequired(totalRequired);
        summary.setSubmittedCount(submittedCount);
        summary.setApprovedCount(approvedCount);
        summary.setRejectedCount(rejectedCount);
        summary.setPendingCount(pendingCount);
        summary.setRemainingRequiredCount(remainingRequiredCount);
        summary.setAllApproved(allApproved);
        summary.setStatusMessage(statusMessage);
        summary.setDocuments(existingDocs.stream().map(entityMapper::toCustomerDocumentDTO).toList());

        return summary;
    }

    @Transactional(readOnly = true)
    public List<CustomerDocumentDTO> getPendingReviewDocuments() {
        if (!hasRole("ADMIN")) {
            throw new AccessDeniedException("Forbidden: Administrator access required");
        }
        return customerDocumentRepository.findByStatusWithCustomer("SUBMITTED").stream()
                .map(entityMapper::toCustomerDocumentDTO)
                .toList();
    }

    public CustomerDocumentDTO reviewDocument(Long id, DocumentReviewDTO reviewDTO) {
        User reviewer = getAuthenticatedUser();
        if (!hasRole("ADMIN")) {
            throw new AccessDeniedException("Forbidden: Administrator access required to review documents");
        }

        CustomerDocument doc = customerDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer document not found with id: " + id));

        String newStatus = reviewDTO.getStatus().trim().toUpperCase();
        if (!"APPROVED".equals(newStatus) && !"REJECTED".equals(newStatus)) {
            throw new IllegalArgumentException("Review status must be either APPROVED or REJECTED");
        }

        doc.setStatus(newStatus);
        if ("REJECTED".equals(newStatus)) {
            doc.setRejectionReason(reviewDTO.getRejectionReason() != null ? reviewDTO.getRejectionReason().trim() : "Document rejected by administrator");
        } else {
            doc.setRejectionReason(null);
        }
        doc.setReviewedBy(reviewer);
        doc.setReviewedAt(LocalDateTime.now());

        CustomerDocument saved = customerDocumentRepository.save(doc);
        CustomerDocumentDTO resDTO = entityMapper.toCustomerDocumentDTO(saved);

        // Real-time broadcast
        realtimeEventPublisher.publishCustomerEvent(saved.getCustomer().getId(), "KYC_DOCUMENT_REVIEWED", resDTO);
        realtimeEventPublisher.publishAdminEvent("KYC_DOCUMENT_REVIEWED", resDTO);

        return resDTO;
    }

    @Transactional(readOnly = true)
    public Resource loadDocumentFile(Long id) {
        User currentUser = getAuthenticatedUser();
        CustomerDocument doc = customerDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));

        // ReBAC check: customer can only download their own document; admin can download all
        if (!hasRole("ADMIN") && !doc.getCustomer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Forbidden: You cannot access documents belonging to other customers");
        }

        try {
            Path file = Paths.get(doc.getFilePath()).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found or unreadable on disk");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed file path", e);
        }
    }
}
