package com.samaksh.farms.inventory.entity;

import com.samaksh.farms.enums.InventoryType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private InventoryType type;

    private Double quantity;

    private String unit;

    private Double unitPrice;

    private String supplier;

    private LocalDateTime purchasedAt;
}