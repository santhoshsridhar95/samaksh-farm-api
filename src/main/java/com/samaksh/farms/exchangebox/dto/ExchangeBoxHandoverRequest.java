package com.samaksh.farms.exchangebox.dto;

import lombok.Data;

@Data
public class ExchangeBoxHandoverRequest {

    private Long collectorUserId;

    private String collectorName;

    private String collectorEmail;

    private String ownerName;

    private Double boxes;

    private String remarks;
}
