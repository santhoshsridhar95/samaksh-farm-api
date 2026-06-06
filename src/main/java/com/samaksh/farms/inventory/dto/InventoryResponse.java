package com.samaksh.farms.inventory.dto;

import com.samaksh.farms.enums.InventoryType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryResponse {

    private Long id;

    private InventoryType type;

    private Double quantity;

    private String unit;

    private Double unitPrice;

    private String supplier;
}