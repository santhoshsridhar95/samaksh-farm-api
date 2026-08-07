package com.samaksh.farms.audit.service;

import com.samaksh.farms.audit.dto.AuditResponse;
import com.samaksh.farms.audit.entity.AuditLog;
import com.samaksh.farms.audit.repo.AuditLogRepository;
import com.samaksh.farms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void createAudit(
            Authentication authentication,
            String module,
            String action,
            String referenceId,
            String remarks
    ) {

        Long userId = 0L;

        String userName = "SYSTEM";

        String userEmail = "SYSTEM";

        if (authentication != null
                && authentication.getPrincipal()
                instanceof User user) {

            userId = user.getId();

            userName = user.getName();

            userEmail = user.getEmail();
        }

        AuditLog auditLog =
                AuditLog.builder()
                        .userId(
                                userId
                        )
                        .userName(
                                userName
                        )
                        .userEmail(
                                userEmail
                        )
                        .module(
                                module
                        )
                        .action(
                                action
                        )
                        .referenceId(
                                referenceId
                        )
                        .remarks(
                                remarks
                        )
                        .createdAt(
                                LocalDateTime.now()
                        )
                        .build();

        auditLogRepository.save(
                auditLog
        );
    }

    public List<AuditResponse> getAuditLogs() {

        return auditLogRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<AuditResponse> getAuditLogsNewestFirst() {

        return auditLogRepository.findAll(
                        Sort.by("createdAt")
                                .descending()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AuditResponse mapToResponse(
            AuditLog auditLog
    ) {

        return AuditResponse.builder()
                .id(
                        auditLog.getId()
                )
                .userId(
                        auditLog.getUserId()
                )
                .userName(
                        auditLog.getUserName()
                )
                .userEmail(
                        auditLog.getUserEmail()
                )
                .module(
                        auditLog.getModule()
                )
                .action(
                        auditLog.getAction()
                )
                .referenceId(
                        auditLog.getReferenceId()
                )
                .remarks(
                        auditLog.getRemarks()
                )
                .createdAt(
                        auditLog.getCreatedAt()
                )
                .build();
    }
}
