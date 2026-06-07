package com.samaksh.farms.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditResponse {

    private Long id;

    private String userEmail;

    private String module;

    private String action;

    private String referenceId;

    private String remarks;

    private LocalDateTime createdAt;
}