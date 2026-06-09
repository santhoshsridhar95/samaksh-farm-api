package com.samaksh.farms.inventory.controller;

import com.samaksh.farms.inventory.dto.InventoryRequest;
import com.samaksh.farms.inventory.dto.InventoryResponse;
import com.samaksh.farms.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','FARM_MANAGER')")
    public InventoryResponse createInventory(
            @RequestBody InventoryRequest request
    ) {

        return inventoryService.createInventory(
                request
        );
    }

    @GetMapping
    public List<InventoryResponse> getInventory() {

        return inventoryService.getAllInventory();
    }
}
