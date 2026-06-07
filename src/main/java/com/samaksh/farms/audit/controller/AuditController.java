package com.samaksh.farms.audit.controller;

import com.samaksh.farms.audit.dto.AuditResponse;
import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','FARM_MANAGER')"
    )
    public ApiResponse<List<AuditResponse>>
    getAuditLogs() {

        return ApiResponse
                .<List<AuditResponse>>builder()
                .success(true)
                .message("Audit logs fetched successfully")
                .data(
                        auditService.getAuditLogs()
                )
                .build();
    }
}