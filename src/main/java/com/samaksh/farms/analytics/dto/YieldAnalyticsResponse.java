package com.samaksh.farms.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YieldAnalyticsResponse {

    private String batchCode;

    private Double spawnUsed;

    private Double totalHarvestKg;

    private Double yieldPerSpawn;
}