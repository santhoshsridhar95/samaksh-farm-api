package com.samaksh.farms.inventorytransaction.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.inventorytransaction.dto.InventoryTransactionRequest;
import com.samaksh.farms.inventorytransaction.dto.InventoryTransactionResponse;
import com.samaksh.farms.inventorytransaction.entity.InventoryTransaction;
import com.samaksh.farms.inventorytransaction.repo.InventoryTransactionRepository;
import com.samaksh.farms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryTransactionService {

    private final InventoryTransactionRepository repository;

    private final AuditService auditService;

    public InventoryTransactionResponse createTransaction(
            InventoryTransactionRequest request,
            Authentication authentication
    ) {

        User loggedInUser =
                (User) authentication.getPrincipal();

        InventoryTransaction transaction =
                InventoryTransaction.builder()
                        .inventoryType(
                                request.getInventoryType()
                        )
                        .transactionType(
                                request.getTransactionType()
                        )
                        .quantity(
                                request.getQuantity()
                        )
                        .remarks(
                                request.getRemarks()
                        )
                        .createdByUserId(
                                loggedInUser.getId()
                        )
                        .createdByEmail(
                                loggedInUser.getEmail()
                        )
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();

        InventoryTransaction saved =
                repository.save(transaction);

        auditService.createAudit(
                authentication,
                "INVENTORY",
                request.getTransactionType().name(),
                request.getInventoryType().name(),
                "Quantity : " + request.getQuantity()
        );

        return mapToResponse(saved);
    }

    public List<InventoryTransactionResponse> getAllTransactions() {

        return repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private InventoryTransactionResponse mapToResponse(
            InventoryTransaction transaction
    ) {

        return InventoryTransactionResponse.builder()
                .id(transaction.getId())
                .inventoryType(
                        transaction.getInventoryType()
                )
                .transactionType(
                        transaction.getTransactionType()
                )
                .quantity(
                        transaction.getQuantity()
                )
                .remarks(
                        transaction.getRemarks()
                )
                .createdByUserId(
                        transaction.getCreatedByUserId()
                )
                .createdByEmail(
                        transaction.getCreatedByEmail()
                )
                .build();
    }
}