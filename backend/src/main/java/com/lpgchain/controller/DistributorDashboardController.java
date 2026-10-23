package com.lpgchain.controller;

import com.lpgchain.dto.DistributorDashboardOverviewDTO;
import com.lpgchain.service.CylinderInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/distributor")
@Tag(name = "Distributor Operations", description = "Operational dashboards and distributor metrics APIs")
public class DistributorDashboardController {

    private final CylinderInventoryService inventoryService;

    public DistributorDashboardController(CylinderInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{distributorId}/overview")
    @Operation(summary = "Get comprehensive operational dashboard summary for a distributor")
    public ResponseEntity<DistributorDashboardOverviewDTO> getOverview(@PathVariable Long distributorId) {
        return ResponseEntity.ok(inventoryService.getDashboardOverview(distributorId));
    }
}
