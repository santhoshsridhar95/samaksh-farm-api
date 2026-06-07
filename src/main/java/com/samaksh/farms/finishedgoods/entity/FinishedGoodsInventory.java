package com.samaksh.farms.finishedgoods.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "finished_goods_inventory")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinishedGoodsInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double availableQuantityKg;
}