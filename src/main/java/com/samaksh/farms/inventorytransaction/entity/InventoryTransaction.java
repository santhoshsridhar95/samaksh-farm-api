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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryType inventoryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Double quantity;

    private String remarks;

    /**
     * Audit Information
     */
    private Long createdByUserId;

    private String createdByEmail;

    private LocalDateTime createdAt;
}