package com.samaksh.farms.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryBalanceResponse {

    private Double spawnBalance;

    private Double pelletBalance;

    private Double bagBalance;

    private Double limeBalance;

    private Double supplementBalance;
}