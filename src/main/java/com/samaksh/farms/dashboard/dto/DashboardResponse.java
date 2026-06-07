package com.samaksh.farms.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardResponse {

    private Long activeBatches;

    private Long totalHarvestEntries;

    private InventoryBalanceResponse inventory;
}