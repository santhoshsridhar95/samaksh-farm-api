package com.samaksh.farms.cashhandover.dto;

import lombok.Data;

@Data
public class CashHandoverRequest {

    private Long collectorUserId;

    private String collectorName;

    private String collectorEmail;

    private String ownerName;

    private Double amount;

    private String remarks;
}
