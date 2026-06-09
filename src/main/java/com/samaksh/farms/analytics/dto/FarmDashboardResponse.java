package com.samaksh.farms.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FarmDashboardResponse {

    private Double spawnBalance;

    private Double pelletBalance;

    private Double bagBalance;

    private Integer darkRoomBags;

    private Integer lightRoomBags;

    private Integer contaminatedBags;

    private Integer discardedBags;
}