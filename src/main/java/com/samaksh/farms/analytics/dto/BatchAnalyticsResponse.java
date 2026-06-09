package com.samaksh.farms.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchAnalyticsResponse {

    private String batchCode;

    private Integer bagsPrepared;

    private Double spawnUsedKg;

    private Double pelletsUsedKg;

    private Double totalHarvestKg;

    private Double yieldPerBag;

    private Double yieldPerSpawnKg;

    private Integer contaminatedBags;

    private Integer discardedBags;
}