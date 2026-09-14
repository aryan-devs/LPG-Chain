package com.lpgchain.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * RbacTestController — Minimal test endpoints for verifying Role-Based Access Control.
 * These endpoints contain no business logic and return simple JSON confirmations.
 */
@RestController
public class RbacTestController {

    @GetMapping("/api/admin/test")
    public ResponseEntity<Map<String, Object>> adminTest() {
        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Admin endpoint accessed successfully"
        ));
    }

    @GetMapping("/api/distributor/test")
    public ResponseEntity<Map<String, Object>> distributorTest() {
        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Distributor endpoint accessed successfully"
        ));
    }

    @GetMapping("/api/customer/test")
    public ResponseEntity<Map<String, Object>> customerTest() {
        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Customer endpoint accessed successfully"
        ));
    }
}
