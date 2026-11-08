package com.samaksh.farms.inventorytransaction.entity;

import com.samaksh.farms.enums.InventoryType;
import com.samaksh.farms.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transaction")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String inventoryType;

    @Column(nullable = false)
    private String transactionType;

    @Column(nullable = false)
    private Double quantity;

    @Column(columnDefinition="text") private String remarks;
    private Integer stockEffect;
    private String productionBatchCode;
    private java.time.LocalDate transactionDate;
    private LocalDateTime updatedAt;
    private String updatedBy;
    public double stockDelta() {
        int direction = stockEffect != null ? stockEffect : ("PURCHASE".equals(transactionType) || "ADJUSTMENT".equals(transactionType) ? 1 : -1);
        return (quantity == null ? 0 : quantity) * direction;
    }

    /**
     * Audit Information
     */
    private Long createdByUserId;

    private String createdByEmail;

    private LocalDateTime createdAt;
}