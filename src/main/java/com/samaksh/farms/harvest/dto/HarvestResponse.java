package com.samaksh.farms.harvest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HarvestResponse {
    private Long performedByEmployeeId;

    private Long id;

    private Long batchId;
    private String batchCode;

    private java.time.LocalDate harvestDate;

    private Double quantity;
    private Integer numberOfBoxes;

    private String remarks;

    private Long createdBy;

    private java.time.LocalDateTime createdAt;
}