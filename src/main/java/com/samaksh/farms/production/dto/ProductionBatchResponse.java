package com.samaksh.farms.production.dto;

import com.samaksh.farms.enums.BatchStatus;
import com.samaksh.farms.enums.MushroomType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductionBatchResponse {

    private Long id;

    private String batchCode;

    private MushroomType mushroomType;

    private Double spawnUsed;

    private Double pelletsUsed;

    private Double bagsUsed;

    private BatchStatus status;
}