package com.samaksh.farms.inventorytransaction.dto;

import com.samaksh.farms.enums.InventoryType;
import com.samaksh.farms.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryTransactionResponse {

    private Long id;

    private InventoryType inventoryType;

    private TransactionType transactionType;

    private Double quantity;

    private String remarks;

    private Long createdByUserId;

    private String createdByEmail;
}