package com.samaksh.farms.inventorytransaction.dto;

import com.samaksh.farms.enums.InventoryType;
import com.samaksh.farms.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryTransactionResponse {
    private Long performedByEmployeeId;

    private Long id;
    private java.time.LocalDate transactionDate;
    private Integer stockEffect;
    private String productionBatchCode;
    private java.time.LocalDateTime createdAt;

    private String inventoryType;

    private String transactionType;

    private Double quantity;

    private String remarks;

    private Long createdByUserId;

    private String createdByEmail;
}