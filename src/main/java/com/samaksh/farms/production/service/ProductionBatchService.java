package com.samaksh.farms.production.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.enums.BatchStatus;
import com.samaksh.farms.enums.InventoryType;
import com.samaksh.farms.enums.MushroomType;
import com.samaksh.farms.enums.TransactionType;
import com.samaksh.farms.inventorytransaction.entity.InventoryTransaction;
import com.samaksh.farms.inventorytransaction.repo.InventoryTransactionRepository;
import com.samaksh.farms.production.dto.ProductionBatchRequest;
import com.samaksh.farms.production.dto.ProductionBatchResponse;
import com.samaksh.farms.production.dto.RoomTransferRequest;
import com.samaksh.farms.production.entity.ProductionBatch;
import com.samaksh.farms.production.repo.ProductionBatchRepository;
import com.samaksh.farms.productionmovement.entity.ProductionMovement;
import com.samaksh.farms.productionmovement.repo.ProductionMovementRepository;
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

    private final ProductionMovementRepository productionMovementRepository;

    private final AuditService auditService;

    public ProductionBatchResponse createBatch(
            ProductionBatchRequest request,
            Authentication authentication
    ) {

        double calculatedSpawnKg =
                request.getBagsPrepared() * 0.150;

        double calculatedPelletsKg =
                request.getBagsPrepared();

        int calculatedCovers =
                request.getBagsPrepared();

        validateInventory(
                calculatedSpawnKg,
                calculatedPelletsKg,
                calculatedCovers
        );

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
                        .bagsPrepared(
                                request.getBagsPrepared()
                        )
                        .damagedCovers(
                                request.getDamagedCovers()
                        )
                        .damagedSpawnKg(
                                request.getDamagedSpawnKg()
                        )
                        .damagedPelletsKg(
                                request.getDamagedPelletsKg()
                        )
                        .spawnUsedKg(
                                calculatedSpawnKg
                        )
                        .pelletsUsedKg(
                                calculatedPelletsKg
                        )
                        .coversUsed(
                                calculatedCovers
                        )
                        .darkRoomBags(
                                request.getBagsPrepared()
                        )
                        .lightRoomBags(
                                0
                        )
                        .contaminatedBags(
                                0
                        )
                        .discardedBags(
                                0
                        )
                        .remarks(
                                request.getRemarks()
                        )
                        .startDate(
                                LocalDate.now()
                        )
                        .status(
                                BatchStatus.ACTIVE
                        )
                        .build();

        ProductionBatch savedBatch =
                productionBatchRepository.save(
                        batch
                );

        createConsumptionTransaction(
                InventoryType.SPAWN,
                calculatedSpawnKg,
                savedBatch.getBatchCode()
        );

        createConsumptionTransaction(
                InventoryType.PELLET,
                calculatedPelletsKg,
                savedBatch.getBatchCode()
        );

        createConsumptionTransaction(
                InventoryType.BAG,
                (double) calculatedCovers,
                savedBatch.getBatchCode()
        );

        createDamageTransaction(
                InventoryType.SPAWN,
                request.getDamagedSpawnKg(),
                savedBatch.getBatchCode()
        );

        createDamageTransaction(
                InventoryType.PELLET,
                request.getDamagedPelletsKg(),
                savedBatch.getBatchCode()
        );

        createDamageTransaction(
                InventoryType.BAG,
                request.getDamagedCovers() == null
                        ? 0.0
                        : request.getDamagedCovers().doubleValue(),
                savedBatch.getBatchCode()
        );

        auditService.createAudit(
                authentication,
                "PRODUCTION",
                "CREATE_BATCH",
                savedBatch.getBatchCode(),
                "Production batch created"
        );

        return mapToResponse(
                savedBatch
        );
    }

    public ProductionBatchResponse transferToLightRoom(
            Long batchId,
            RoomTransferRequest request,
            Authentication authentication
    ) {

        ProductionBatch batch =
                productionBatchRepository.findById(
                        batchId
                ).orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Batch",
                                batchId
                        )
                );

        int moved =
                request.getMovedToLightRoom() == null
                        ? 0
                        : request.getMovedToLightRoom();

        int contaminated =
                request.getContaminatedBags() == null
                        ? 0
                        : request.getContaminatedBags();

        int discarded =
                request.getDiscardedBags() == null
                        ? 0
                        : request.getDiscardedBags();

        int totalProcessed =
                moved
                        + contaminated
                        + discarded;

        if (totalProcessed > batch.getDarkRoomBags()) {

            throw new RuntimeException(
                    "Processed bags exceed available dark room bags"
            );
        }

        batch.setDarkRoomBags(
                batch.getDarkRoomBags()
                        - totalProcessed
        );

        batch.setLightRoomBags(
                batch.getLightRoomBags()
                        + moved
        );

        batch.setContaminatedBags(
                batch.getContaminatedBags()
                        + contaminated
        );

        batch.setDiscardedBags(
                batch.getDiscardedBags()
                        + discarded
        );

        ProductionBatch savedBatch =
                productionBatchRepository.save(
                        batch
                );

        ProductionMovement movement =
                ProductionMovement.builder()
                        .batch(
                                savedBatch
                        )
                        .movedToLightRoom(
                                moved
                        )
                        .contaminatedBags(
                                contaminated
                        )
                        .discardedBags(
                                discarded
                        )
                        .remarks(
                                request.getRemarks()
                        )
                        .movementDate(
                                LocalDateTime.now()
                        )
                        .build();

        productionMovementRepository.save(
                movement
        );

        auditService.createAudit(
                authentication,
                "PRODUCTION",
                "TRANSFER_TO_LIGHT_ROOM",
                savedBatch.getBatchCode(),
                request.getRemarks()
        );

        return mapToResponse(
                savedBatch
        );
    }

    public List<ProductionBatchResponse> getAllBatches() {

        return productionBatchRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private double getAvailableInventory(
            InventoryType inventoryType
    ) {

        List<InventoryTransaction> transactions =
                inventoryTransactionRepository
                        .findByInventoryType(
                                inventoryType
                        );

        double available = 0;

        for (InventoryTransaction transaction
                : transactions) {

            switch (
                    transaction.getTransactionType()
            ) {

                case PURCHASE ->

                        available +=
                                transaction.getQuantity();

                case CONSUMPTION,
                     DAMAGE ->

                        available -=
                                transaction.getQuantity();
            }
        }

        return available;
    }

    private void validateInventory(
            double requiredSpawn,
            double requiredPellets,
            double requiredBags
    ) {

        double availableSpawn =
                getAvailableInventory(
                        InventoryType.SPAWN
                );

        double availablePellets =
                getAvailableInventory(
                        InventoryType.PELLET
                );

        double availableBags =
                getAvailableInventory(
                        InventoryType.BAG
                );

        if (availableSpawn < requiredSpawn) {

            throw new RuntimeException(
                    "Insufficient Spawn Stock. Available : "
                            + availableSpawn
                            + " KG, Required : "
                            + requiredSpawn
                            + " KG"
            );
        }

        if (availablePellets < requiredPellets) {

            throw new RuntimeException(
                    "Insufficient Pellet Stock. Available : "
                            + availablePellets
                            + " KG, Required : "
                            + requiredPellets
                            + " KG"
            );
        }

        if (availableBags < requiredBags) {

            throw new RuntimeException(
                    "Insufficient Bag Stock. Available : "
                            + availableBags
                            + ", Required : "
                            + requiredBags
            );
        }
    }

    private ProductionBatchResponse mapToResponse(
            ProductionBatch batch
    ) {

        return ProductionBatchResponse.builder()
                .id(batch.getId())
                .batchCode(batch.getBatchCode())
                .mushroomType(batch.getMushroomType())
                .spawnUsed(batch.getSpawnUsedKg())
                .pelletsUsed(batch.getPelletsUsedKg())
                .bagsUsed(
                        batch.getBagsPrepared() == null
                                ? 0.0
                                : batch.getBagsPrepared().doubleValue()
                )
                .darkRoomBags(batch.getDarkRoomBags())
                .lightRoomBags(batch.getLightRoomBags())
                .contaminatedBags(batch.getContaminatedBags())
                .discardedBags(batch.getDiscardedBags())
                .status(batch.getStatus())
                .build();
    }

    private void createConsumptionTransaction(
            InventoryType inventoryType,
            Double quantity,
            String batchCode
    ) {

        if (quantity == null || quantity <= 0) {
            return;
        }

        inventoryTransactionRepository.save(
                InventoryTransaction.builder()
                        .inventoryType(inventoryType)
                        .transactionType(TransactionType.CONSUMPTION)
                        .quantity(quantity)
                        .remarks("Consumed in batch " + batchCode)
                        .createdByUserId(0L)
                        .createdByEmail("SYSTEM")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    private void createDamageTransaction(
            InventoryType inventoryType,
            Double quantity,
            String batchCode
    ) {

        if (quantity == null || quantity <= 0) {
            return;
        }

        inventoryTransactionRepository.save(
                InventoryTransaction.builder()
                        .inventoryType(inventoryType)
                        .transactionType(TransactionType.DAMAGE)
                        .quantity(quantity)
                        .remarks("Damaged during batch " + batchCode)
                        .createdByUserId(0L)
                        .createdByEmail("SYSTEM")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
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
                "-"
                + date
                + "-"
                + String.format("%03d", count);
    }
}