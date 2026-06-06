package com.samaksh.farms.inventory.service;

import com.samaksh.farms.inventory.dto.InventoryRequest;
import com.samaksh.farms.inventory.dto.InventoryResponse;
import com.samaksh.farms.inventory.entity.Inventory;
import com.samaksh.farms.inventory.repo.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryResponse createInventory(
            InventoryRequest request
    ) {

        Inventory inventory = Inventory.builder()
                .type(request.getType())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .unitPrice(request.getUnitPrice())
                .supplier(request.getSupplier())
                .purchasedAt(LocalDateTime.now())
                .build();

        Inventory savedInventory =
                inventoryRepository.save(inventory);

        return mapToResponse(savedInventory);
    }

    public List<InventoryResponse> getAllInventory() {

        return inventoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private InventoryResponse mapToResponse(
            Inventory inventory
    ) {

        return InventoryResponse.builder()
                .id(inventory.getId())
                .type(inventory.getType())
                .quantity(inventory.getQuantity())
                .unit(inventory.getUnit())
                .unitPrice(inventory.getUnitPrice())
                .supplier(inventory.getSupplier())
                .build();
    }
}