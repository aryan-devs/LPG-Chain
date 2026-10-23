package com.lpgchain.controller;

import com.lpgchain.config.OpenApiConfig;
import com.lpgchain.dto.CustomerProfileDTO;
import com.lpgchain.dto.SavedAddressDTO;
import com.lpgchain.service.CustomerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@Tag(name = "Customer Profile & Addresses", description = "Endpoints for managing customer profile and delivery address book")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    public CustomerProfileController(CustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current authenticated customer profile")
    public ResponseEntity<CustomerProfileDTO> getProfile() {
        return ResponseEntity.ok(customerProfileService.getProfile());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current authenticated customer profile")
    public ResponseEntity<CustomerProfileDTO> updateProfile(@RequestBody CustomerProfileDTO dto) {
        return ResponseEntity.ok(customerProfileService.updateProfile(dto));
    }

    @GetMapping("/addresses")
    @Operation(summary = "Get list of saved delivery addresses for current customer")
    public ResponseEntity<List<SavedAddressDTO>> getAddresses() {
        return ResponseEntity.ok(customerProfileService.getSavedAddresses());
    }

    @PostMapping("/addresses")
    @Operation(summary = "Add a new saved delivery address")
    public ResponseEntity<SavedAddressDTO> addAddress(@Valid @RequestBody SavedAddressDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerProfileService.addSavedAddress(dto));
    }

    @PutMapping("/addresses/{id}")
    @Operation(summary = "Update an existing saved delivery address")
    public ResponseEntity<SavedAddressDTO> updateAddress(@PathVariable Long id, @Valid @RequestBody SavedAddressDTO dto) {
        return ResponseEntity.ok(customerProfileService.updateSavedAddress(id, dto));
    }

    @PutMapping("/addresses/{id}/default")
    @Operation(summary = "Set an address as default delivery location")
    public ResponseEntity<Void> setDefaultAddress(@PathVariable Long id) {
        customerProfileService.setDefaultAddress(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/addresses/{id}")
    @Operation(summary = "Delete a saved delivery address")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id) {
        customerProfileService.deleteSavedAddress(id);
        return ResponseEntity.noContent().build();
    }
}
