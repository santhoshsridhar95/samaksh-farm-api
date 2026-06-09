package com.samaksh.farms.production.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class RoomTransferRequest {

    @Min(0)
    private Integer movedToLightRoom;

    @Min(0)
    private Integer contaminatedBags;

    @Min(0)
    private Integer discardedBags;

    private String remarks;
}