package com.samaksh.farms.harvest.dto;

import lombok.Data;

@Data
public class HarvestRequest {
    private Long performedByEmployeeId;

    private Long batchId;

    private java.time.LocalDate harvestDate;

    private Double quantity;
    private Integer numberOfBoxes;

    private String remarks;
}