package com.samaksh.farms.inventorytransaction.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.inventorytransaction.dto.InventoryTransactionRequest;
import com.samaksh.farms.inventorytransaction.dto.InventoryTransactionResponse;
import com.samaksh.farms.inventorytransaction.service.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-transactions")
@RequiredArgsConstructor
public class InventoryTransactionController {

    private final InventoryTransactionService service;

    @PostMapping
    public ApiResponse<InventoryTransactionResponse>
    createTransaction(
            @RequestBody InventoryTransactionRequest request,
            Authentication authentication
    ) {

        return ApiResponse
                .<InventoryTransactionResponse>builder()
                .success(true)
                .message(
                        "Inventory transaction created successfully"
                )
                .data(
                        service.createTransaction(
                                request,
                                authentication
                        )
                )
                .build();
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<InventoryTransactionResponse> edit(@PathVariable Long id, @RequestBody InventoryTransactionRequest r, Authentication a) {
        return ApiResponse.<InventoryTransactionResponse>builder().success(true).message("Inventory updated").data(service.edit(id,r,a)).build();
    }
    @GetMapping
    public ApiResponse<List<InventoryTransactionResponse>>
    getTransactions() {

        return ApiResponse
                .<List<InventoryTransactionResponse>>builder()
                .success(true)
                .message(
                        "Inventory transactions fetched successfully"
                )
                .data(
                        service.getAllTransactions()
                )
                .build();
    }
}