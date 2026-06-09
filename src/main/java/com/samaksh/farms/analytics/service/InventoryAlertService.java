package com.samaksh.farms.analytics.service;

import com.samaksh.farms.analytics.dto.InventoryAlertResponse;
import com.samaksh.farms.enums.InventoryType;
import com.samaksh.farms.enums.TransactionType;
import com.samaksh.farms.inventorytransaction.entity.InventoryTransaction;
import com.samaksh.farms.inventorytransaction.repo.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryAlertService {

    private final InventoryTransactionRepository repository;

    public List<InventoryAlertResponse> getAlerts() {

        List<InventoryAlertResponse> alerts =
                new ArrayList<>();

        alerts.add(
                buildAlert(
                        InventoryType.SPAWN,
                        10.0
                )
        );

        alerts.add(
                buildAlert(
                        InventoryType.PELLET,
                        100.0
                )
        );

        alerts.add(
                buildAlert(
                        InventoryType.BAG,
                        200.0
                )
        );

        return alerts;
    }

    private InventoryAlertResponse buildAlert(
            InventoryType inventoryType,
            Double minimumRequired
    ) {

        double balance =
                repository.findByInventoryType(
                                inventoryType
                        )
                        .stream()
                        .mapToDouble(this::calculate)
                        .sum();

        return InventoryAlertResponse.builder()
                .inventoryType(
                        inventoryType.name()
                )
                .currentBalance(
                        balance
                )
                .minimumRequired(
                        minimumRequired
                )
                .alertStatus(
                        balance < minimumRequired
                                ? "LOW_STOCK"
                                : "OK"
                )
                .build();
    }

    private double calculate(
            InventoryTransaction transaction
    ) {

        if (transaction.getTransactionType()
                == TransactionType.PURCHASE) {

            return transaction.getQuantity();
        }

        return -transaction.getQuantity();
    }
}