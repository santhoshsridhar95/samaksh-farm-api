package com.samaksh.farms.exchangebox.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExchangeBoxHandoverResponse {

    private Long id;

    private Long collectorUserId;

    private String collectorName;

    private String collectorEmail;

    private String ownerName;

    private Double boxes;

    private String remarks;

    private String recordedByName;

    private String recordedByEmail;

    private LocalDateTime receivedAt;
}
