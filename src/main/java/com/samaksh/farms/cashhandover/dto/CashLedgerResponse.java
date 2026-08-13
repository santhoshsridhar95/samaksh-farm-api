package com.samaksh.farms.cashhandover.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CashLedgerResponse {

    private List<CashCollectionSummaryResponse> summaries;

    private List<CashHandoverResponse> handovers;
}
