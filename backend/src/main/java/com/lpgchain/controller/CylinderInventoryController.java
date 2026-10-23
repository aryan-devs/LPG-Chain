package com.lpgchain.controller;

import com.lpgchain.dto.CylinderInventoryResponseDTO;
import com.lpgchain.dto.CylinderInventoryUpdateDTO;
import com.lpgchain.dto.InventoryMovementResponseDTO;
import com.lpgchain.service.CylinderInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Cylinder Inventory", description = "Depot physical inventory and movement auditing APIs")
public class CylinderInventoryController {

    private final CylinderInventoryService inventoryService;

    public CylinderInventoryController(CylinderInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/distributor/{distributorId}")
    @Operation(summary = "Get category-wise physical depot inventory for a distributor")
    public ResponseEntity<List<CylinderInventoryResponseDTO>> getDistributorInventory(@PathVariable Long distributorId) {
        return ResponseEntity.ok(inventoryService.getDistributorInventory(distributorId));
    }

    @PutMapping("/distributor/{distributorId}")
    @Operation(summary = "Update physical stock counts and log auditable movement")
    public ResponseEntity<CylinderInventoryResponseDTO> updateStock(
            @PathVariable Long distributorId,
            @Valid @RequestBody CylinderInventoryUpdateDTO dto) {
        return ResponseEntity.ok(inventoryService.updateStock(distributorId, dto));
    }

    @GetMapping("/distributor/{distributorId}/movements")
    @Operation(summary = "Get stock adjustment audit movements for a category")
    public ResponseEntity<List<InventoryMovementResponseDTO>> getMovements(
            @PathVariable Long distributorId,
            @RequestParam(required = false, defaultValue = "DOMESTIC_14_2") String category) {
        return ResponseEntity.ok(inventoryService.getInventoryMovements(distributorId, category));
    }
}
