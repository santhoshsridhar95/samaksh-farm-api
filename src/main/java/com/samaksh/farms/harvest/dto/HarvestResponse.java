package com.samaksh.farms.harvest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HarvestResponse {

    private Long id;

    private String batchCode;

    private Double quantity;

    private String remarks;
}