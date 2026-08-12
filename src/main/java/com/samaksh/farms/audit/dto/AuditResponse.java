package com.samaksh.farms.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditResponse {

    private Long id;

    private Long userId;

    private String userName;

    private String userEmail;

    private String module;

    private String action;

    private String referenceId;

    private String remarks;

    private LocalDateTime createdAt;
}
