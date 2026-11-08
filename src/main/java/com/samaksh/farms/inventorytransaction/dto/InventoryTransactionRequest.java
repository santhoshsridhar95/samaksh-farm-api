package com.samaksh.farms.inventorytransaction.dto;

import com.samaksh.farms.enums.InventoryType;
import com.samaksh.farms.enums.TransactionType;
import lombok.Data;

@Data
public class InventoryTransactionRequest {

    private String inventoryType;

    private String transactionType;

    private Double quantity;

    private String remarks;
    private java.time.LocalDate transactionDate;
}