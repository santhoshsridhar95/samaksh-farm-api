package com.samaksh.farms.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryAlertResponse {

    private String inventoryType;

    private Double currentBalance;

    private Double minimumRequired;

    private String alertStatus;
}