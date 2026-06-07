package com.samaksh.farms.production.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.enums.BatchStatus;
import com.samaksh.farms.enums.InventoryType;
import com.samaksh.farms.enums.MushroomType;
import com.samaksh.farms.enums.TransactionType;
import com.samaksh.farms.inventorytransaction.entity.InventoryTransaction;
import com.samaksh.farms.inventorytransaction.repo.InventoryTransactionRepository;
import com.samaksh.farms.production.dto.ProductionBatchRequest;
import com.samaksh.farms.production.dto.ProductionBatchResponse;
import com.samaksh.farms.production.entity.ProductionBatch;
import com.samaksh.farms.production.repo.ProductionBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductionBatchService {

    private final ProductionBatchRepository productionBatchRepository;

    private final InventoryTransactionRepository inventoryTransactionRepository;

    private final AuditService auditService;

    public ProductionBatchResponse createBatch(
            ProductionBatchRequest request,
            Authentication authentication
    ) {

        ProductionBatch batch =
                ProductionBatch.builder()
                        .batchCode(
                                generateBatchCode(
                                        request.getMushroomType()
                                )
                        )
                        .mushroomType(
                                request.getMushroomType()
                        )
                        .spawnUsed(
                                request.getSpawnUsed()
                        )
                        .pelletsUsed(
                                request.getPelletsUsed()
                        )
                        .bagsUsed(
                                request.getBagsUsed()
                        )
                        .startDate(
                                LocalDate.now()
                        )
                        .status(
                                BatchStatus.ACTIVE
                        )
                        .build();

        ProductionBatch savedBatch =
                productionBatchRepository.save(batch);

        createConsumptionTransaction(
                InventoryType.SPAWN,
                request.getSpawnUsed(),
                savedBatch.getBatchCode()
        );

        createConsumptionTransaction(
                InventoryType.PELLET,
                request.getPelletsUsed(),
                savedBatch.getBatchCode()
        );

        createConsumptionTransaction(
                InventoryType.BAG,
                request.getBagsUsed(),
                savedBatch.getBatchCode()
        );

        auditService.createAudit(
                authentication,
                "PRODUCTION",
                "CREATE_BATCH",
                savedBatch.getBatchCode(),
                "Production batch created"
        );

        return mapToResponse(savedBatch);
    }

    public List<ProductionBatchResponse> getAllBatches() {

        return productionBatchRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void createConsumptionTransaction(
            InventoryType inventoryType,
            Double quantity,
            String batchCode
    ) {

        if (quantity == null || quantity <= 0) {
            return;
        }

        InventoryTransaction transaction =
                InventoryTransaction.builder()
                        .inventoryType(
                                inventoryType
                        )
                        .transactionType(
                                TransactionType.CONSUMPTION
                        )
                        .quantity(
                                quantity
                        )
                        .remarks(
                                "Consumed in batch "
                                        + batchCode
                        )
                        .createdByUserId(
                                0L
                        )
                        .createdByEmail(
                                "SYSTEM"
                        )
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();

        inventoryTransactionRepository.save(
                transaction
        );
    }

    private ProductionBatchResponse mapToResponse(
            ProductionBatch batch
    ) {

        return ProductionBatchResponse.builder()
                .id(batch.getId())
                .batchCode(batch.getBatchCode())
                .mushroomType(batch.getMushroomType())
                .spawnUsed(batch.getSpawnUsed())
                .pelletsUsed(batch.getPelletsUsed())
                .bagsUsed(batch.getBagsUsed())
                .status(batch.getStatus())
                .build();
    }

    private String generateBatchCode(
            MushroomType type
    ) {

        String prefix =
                switch (type) {

                    case OYSTER -> "SF-OYS";

                    case BUTTON -> "SF-BTN";

                    case MILKY -> "SF-MLK";
                };

        String date =
                LocalDate.now()
                        .toString()
                        .replace("-", "");

        long count =
                productionBatchRepository.count() + 1;

        return prefix +
                "-" +
                date +
                "-" +
                String.format(
                        "%03d",
                        count
                );
    }
}