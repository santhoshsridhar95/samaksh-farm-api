package com.samaksh.farms.harvest.dto;

import lombok.Data;

@Data
public class HarvestRequest {

    private Long batchId;

    private Double quantity;

    private String remarks;
}