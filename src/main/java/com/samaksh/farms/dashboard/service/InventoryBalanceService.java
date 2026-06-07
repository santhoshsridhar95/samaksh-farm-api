package com.samaksh.farms.dashboard.service;

import com.samaksh.farms.dashboard.dto.InventoryBalanceResponse;
import com.samaksh.farms.enums.InventoryType;
import com.samaksh.farms.enums.TransactionType;
import com.samaksh.farms.inventorytransaction.entity.InventoryTransaction;
import com.samaksh.farms.inventorytransaction.repo.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryBalanceService {

    private final InventoryTransactionRepository repository;

    public InventoryBalanceResponse getInventoryBalance() {

        return InventoryBalanceResponse.builder()
                .spawnBalance(
                        calculateBalance(
                                InventoryType.SPAWN
                        )
                )
                .pelletBalance(
                        calculateBalance(
                                InventoryType.PELLET
                        )
                )
                .bagBalance(
                        calculateBalance(
                                InventoryType.BAG
                        )
                )
                .limeBalance(
                        calculateBalance(
                                InventoryType.LIME
                        )
                )
                .supplementBalance(
                        calculateBalance(
                                InventoryType.SUPPLEMENT
                        )
                )
                .build();
    }

    private Double calculateBalance(
            InventoryType type
    ) {

        List<InventoryTransaction> transactions =
                repository.findByInventoryType(type);

        double balance = 0;

        for (InventoryTransaction tx : transactions) {

            if (tx.getTransactionType() ==
                    TransactionType.PURCHASE) {

                balance += tx.getQuantity();
            }

            else if (tx.getTransactionType() ==
                    TransactionType.CONSUMPTION) {

                balance -= tx.getQuantity();
            }

            else if (tx.getTransactionType() ==
                    TransactionType.ADJUSTMENT) {

                balance += tx.getQuantity();
            }
        }

        return balance;
    }
}