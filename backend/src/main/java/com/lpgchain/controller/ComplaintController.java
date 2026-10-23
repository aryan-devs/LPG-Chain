package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.dto.ComplaintRequestDTO;
import com.lpgchain.dto.ComplaintResponseDTO;
import com.lpgchain.dto.ComplaintStatusUpdateDTO;
import com.lpgchain.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@Tag(name = "Complaints Management", description = "Endpoints for customer grievance submission, agency tracking, and administrative resolution")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping
    @Operation(summary = "Submit a new customer complaint")
    public ResponseEntity<ComplaintResponseDTO> createComplaint(@Valid @RequestBody ComplaintRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(complaintService.createComplaint(request));
    }

    @GetMapping("/my")
    @Operation(summary = "Get complaint history for current customer")
    public ResponseEntity<List<ComplaintResponseDTO>> getMyComplaints() {
        return ResponseEntity.ok(complaintService.getMyComplaints());
    }

    @GetMapping("/distributor/{distributorId}")
    @Operation(summary = "Get complaints assigned to distributor agency")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN')")
    public ResponseEntity<List<ComplaintResponseDTO>> getDistributorComplaints(@PathVariable Long distributorId) {
        return ResponseEntity.ok(complaintService.getDistributorComplaints(distributorId));
    }

    @GetMapping("/admin")
    @Operation(summary = "Get platform-wide complaints with optional status filter (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ComplaintResponseDTO>> getAllComplaints(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(complaintService.getAllComplaints(status));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update complaint status and append resolution note")
    @PreAuthorize("hasAnyRole('DISTRIBUTOR', 'ADMIN')")
    public ResponseEntity<ComplaintResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintStatusUpdateDTO dto) {
        return ResponseEntity.ok(complaintService.updateComplaintStatus(id, dto));
    }
}
