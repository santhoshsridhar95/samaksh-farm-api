package com.samaksh.farms.harvest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HarvestResponse {

    private Long id;

    private Long batchId;
    private String batchCode;

    private java.time.LocalDate harvestDate;

    private Double quantity;

    private String remarks;
}