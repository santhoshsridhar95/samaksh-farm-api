package com.samaksh.farms.cashhandover.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CashHandoverResponse {

    private Long id;

    private Long collectorUserId;

    private String collectorName;

    private String collectorEmail;

    private String ownerName;

    private Double amount;

    private String remarks;

    private String recordedByName;

    private String recordedByEmail;

    private LocalDateTime handedOverAt;

    private LocalDateTime updatedAt;
}
