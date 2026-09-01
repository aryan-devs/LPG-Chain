package com.lpgchain.controller;

import com.lpgchain.entity.DistributorMembership;
import com.lpgchain.service.DistributorMembershipService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/memberships")
public class DistributorMembershipController {

    private final DistributorMembershipService distributorMembershipService;

    public DistributorMembershipController(DistributorMembershipService distributorMembershipService) {
        this.distributorMembershipService = distributorMembershipService;
    }

    @GetMapping
    public ResponseEntity<List<DistributorMembership>> getAllMemberships() {
        return ResponseEntity.ok(distributorMembershipService.getAllMemberships());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DistributorMembership> getMembershipById(@PathVariable Long id) {
        return distributorMembershipService.getMembershipById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DistributorMembership> createMembership(@RequestBody DistributorMembership membership) {
        DistributorMembership savedMembership = distributorMembershipService.saveMembership(membership);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedMembership);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMembership(@PathVariable Long id) {
        distributorMembershipService.deleteMembership(id);
        return ResponseEntity.noContent().build();
    }
}
