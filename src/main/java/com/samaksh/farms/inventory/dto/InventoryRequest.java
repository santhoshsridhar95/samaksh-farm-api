package com.samaksh.farms.inventory.dto;

import com.samaksh.farms.enums.InventoryType;
import lombok.Data;

@Data
public class InventoryRequest {

    private InventoryType type;

    private Double quantity;

    private String unit;

    private Double unitPrice;

    private String supplier;
}