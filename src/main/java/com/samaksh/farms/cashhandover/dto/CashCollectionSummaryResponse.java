package com.samaksh.farms.cashhandover.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CashCollectionSummaryResponse {

    private Long collectorUserId;

    private String collectorName;

    private String collectorEmail;

    private Double todayCollected;

    private Double totalCollected;

    private Double totalHandedOver;

    private Double balanceWithUser;
}
