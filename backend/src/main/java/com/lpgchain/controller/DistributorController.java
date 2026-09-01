package com.lpgchain.controller;

import com.lpgchain.entity.Distributor;
import com.lpgchain.service.DistributorService;
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
@RequestMapping("/api/distributors")
public class DistributorController {

    private final DistributorService distributorService;

    public DistributorController(DistributorService distributorService) {
        this.distributorService = distributorService;
    }

    @GetMapping
    public ResponseEntity<List<Distributor>> getAllDistributors() {
        return ResponseEntity.ok(distributorService.getAllDistributors());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Distributor> getDistributorById(@PathVariable Long id) {
        return distributorService.getDistributorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Distributor> createDistributor(@RequestBody Distributor distributor) {
        Distributor savedDistributor = distributorService.saveDistributor(distributor);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDistributor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDistributor(@PathVariable Long id) {
        distributorService.deleteDistributor(id);
        return ResponseEntity.noContent().build();
    }
}
