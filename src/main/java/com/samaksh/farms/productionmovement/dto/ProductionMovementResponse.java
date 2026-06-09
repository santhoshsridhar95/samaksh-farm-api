package com.samaksh.farms.productionmovement.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductionMovementResponse {

    private Long id;

    private String batchCode;

    private Integer movedToLightRoom;

    private Integer contaminatedBags;

    private Integer discardedBags;

    private String remarks;

    private LocalDateTime movementDate;
}