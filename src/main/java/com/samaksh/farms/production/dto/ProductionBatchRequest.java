package com.samaksh.farms.production.dto;

import com.samaksh.farms.enums.MushroomType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProductionBatchRequest {

    @NotNull
    private MushroomType mushroomType;

    @NotNull
    @Positive
    private Integer bagsPrepared;

    private Integer damagedCovers;

    private Double damagedSpawnKg;

    private Double damagedPelletsKg;

    private String remarks;
}