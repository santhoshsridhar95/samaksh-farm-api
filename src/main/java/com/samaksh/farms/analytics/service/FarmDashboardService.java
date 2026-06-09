package com.samaksh.farms.analytics.service;

import com.samaksh.farms.analytics.dto.FarmDashboardResponse;
import com.samaksh.farms.enums.InventoryType;
import com.samaksh.farms.enums.TransactionType;
import com.samaksh.farms.inventorytransaction.entity.InventoryTransaction;
import com.samaksh.farms.inventorytransaction.repo.InventoryTransactionRepository;
import com.samaksh.farms.production.repo.ProductionBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FarmDashboardService {

    private final InventoryTransactionRepository inventoryRepository;

    private final ProductionBatchRepository batchRepository;

    public FarmDashboardResponse getDashboard() {

        double spawnBalance =
                calculateInventoryBalance(
                        InventoryType.SPAWN
                );

        double pelletBalance =
                calculateInventoryBalance(
                        InventoryType.PELLET
                );

        double bagBalance =
                calculateInventoryBalance(
                        InventoryType.BAG
                );

        int darkRoom =
                batchRepository.findAll()
                        .stream()
                        .mapToInt(
                                batch ->
                                        batch.getDarkRoomBags() == null
                                                ? 0
                                                : batch.getDarkRoomBags()
                        )
                        .sum();

        int lightRoom =
                batchRepository.findAll()
                        .stream()
                        .mapToInt(
                                batch ->
                                        batch.getLightRoomBags() == null
                                                ? 0
                                                : batch.getLightRoomBags()
                        )
                        .sum();

        int contaminated =
                batchRepository.findAll()
                        .stream()
                        .mapToInt(
                                batch ->
                                        batch.getContaminatedBags() == null
                                                ? 0
                                                : batch.getContaminatedBags()
                        )
                        .sum();

        int discarded =
                batchRepository.findAll()
                        .stream()
                        .mapToInt(
                                batch ->
                                        batch.getDiscardedBags() == null
                                                ? 0
                                                : batch.getDiscardedBags()
                        )
                        .sum();

        return FarmDashboardResponse.builder()
                .spawnBalance(spawnBalance)
                .pelletBalance(pelletBalance)
                .bagBalance(bagBalance)
                .darkRoomBags(darkRoom)
                .lightRoomBags(lightRoom)
                .contaminatedBags(contaminated)
                .discardedBags(discarded)
                .build();
    }

    private double calculateInventoryBalance(
            InventoryType inventoryType
    ) {

        return inventoryRepository
                .findByInventoryType(
                        inventoryType
                )
                .stream()
                .mapToDouble(transaction -> {

                    if (transaction.getTransactionType()
                            == TransactionType.PURCHASE) {

                        return transaction.getQuantity();
                    }

                    return -transaction.getQuantity();
                })
                .sum();
    }
}