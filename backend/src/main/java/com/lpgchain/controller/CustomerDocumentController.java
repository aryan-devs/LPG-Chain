package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.dto.CustomerDocumentDTO;
import com.lpgchain.dto.DocumentReviewDTO;
import com.lpgchain.dto.KycSummaryDTO;
import com.lpgchain.service.CustomerDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "KYC & Compliance Documents", description = "Endpoints for customer KYC document uploads, requirement tracking, and administrative review")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class CustomerDocumentController {

    private final CustomerDocumentService customerDocumentService;

    public CustomerDocumentController(CustomerDocumentService customerDocumentService) {
        this.customerDocumentService = customerDocumentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a KYC compliance document (Aadhaar, Ration Card, Electricity Bill, Bank Passbook)")
    public ResponseEntity<CustomerDocumentDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "documentNumber", required = false) String documentNumber) {
        CustomerDocumentDTO dto = customerDocumentService.uploadDocument(file, documentType, documentNumber);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get current customer's KYC compliance summary and remaining upload checklist")
    public ResponseEntity<KycSummaryDTO> getKycSummary() {
        return ResponseEntity.ok(customerDocumentService.getKycSummary());
    }

    @GetMapping("/admin/pending")
    @Operation(summary = "Get list of pending documents awaiting verification (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CustomerDocumentDTO>> getPendingDocuments() {
        return ResponseEntity.ok(customerDocumentService.getPendingReviewDocuments());
    }

    @PutMapping({"/admin/{id}/review", "/{id}/status"})
    @Operation(summary = "Approve or reject a submitted customer document (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerDocumentDTO> reviewDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentReviewDTO reviewDTO) {
        return ResponseEntity.ok(customerDocumentService.reviewDocument(id, reviewDTO));
    }

    @GetMapping("/{id}/file")
    @Operation(summary = "Download a secure document file (Owner or Admin only)")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Resource file = customerDocumentService.loadDocumentFile(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }
}
