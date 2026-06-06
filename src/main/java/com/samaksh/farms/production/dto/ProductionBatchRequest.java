package com.samaksh.farms.production.dto;

import com.samaksh.farms.enums.MushroomType;
import lombok.Data;

@Data
public class ProductionBatchRequest {

    private MushroomType mushroomType;

    private Double spawnUsed;

    private Double pelletsUsed;

    private Double bagsUsed;
}