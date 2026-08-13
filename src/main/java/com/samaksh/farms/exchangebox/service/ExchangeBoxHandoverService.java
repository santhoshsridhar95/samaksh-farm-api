package com.samaksh.farms.exchangebox.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.time.BusinessTime;
import com.samaksh.farms.exchangebox.dto.ExchangeBoxHandoverRequest;
import com.samaksh.farms.exchangebox.dto.ExchangeBoxHandoverResponse;
import com.samaksh.farms.exchangebox.entity.ExchangeBoxHandover;
import com.samaksh.farms.exchangebox.repo.ExchangeBoxHandoverRepository;
import com.samaksh.farms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExchangeBoxHandoverService {

    private final ExchangeBoxHandoverRepository repository;

    private final AuditService auditService;

    public List<ExchangeBoxHandoverResponse> getHandovers() {

        return repository.findAllByOrderByReceivedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ExchangeBoxHandoverResponse createHandover(
            ExchangeBoxHandoverRequest request,
            Authentication authentication
    ) {

        validate(request);

        ExchangeBoxHandover handover =
                ExchangeBoxHandover.builder()
                        .collectorUserId(request.getCollectorUserId())
                        .collectorName(requiredText(
                                request.getCollectorName(),
                                "Collector name is required"
                        ))
                        .collectorEmail(optionalText(request.getCollectorEmail()))
                        .ownerName(requiredText(
                                request.getOwnerName(),
                                "Owner name is required"
                        ))
                        .boxes(request.getBoxes())
                        .remarks(optionalText(request.getRemarks()))
                        .recordedByUserId(currentUserId(authentication))
                        .recordedByName(currentUserName(authentication))
                        .recordedByEmail(currentUserEmail(authentication))
                        .receivedAt(BusinessTime.now())
                        .build();

        ExchangeBoxHandover saved =
                repository.save(handover);

        auditService.createAudit(
                authentication,
                "EXCHANGE_BOX",
                "RECEIVE_EXCHANGE_BOXES",
                saved.getCollectorName(),
                "Exchange boxes received by "
                        + saved.getOwnerName()
                        + ": "
                        + saved.getBoxes()
        );

        return mapToResponse(saved);
    }

    private void validate(
            ExchangeBoxHandoverRequest request
    ) {

        if (request.getBoxes() == null ||
                request.getBoxes() <= 0) {
            throw new IllegalArgumentException(
                    "Received boxes must be greater than 0"
            );
        }

        requiredText(
                request.getCollectorName(),
                "Collector name is required"
        );
        requiredText(
                request.getOwnerName(),
                "Owner name is required"
        );
    }

    private ExchangeBoxHandoverResponse mapToResponse(
            ExchangeBoxHandover handover
    ) {

        return ExchangeBoxHandoverResponse.builder()
                .id(handover.getId())
                .collectorUserId(handover.getCollectorUserId())
                .collectorName(handover.getCollectorName())
                .collectorEmail(handover.getCollectorEmail())
                .ownerName(handover.getOwnerName())
                .boxes(handover.getBoxes())
                .remarks(handover.getRemarks())
                .recordedByName(handover.getRecordedByName())
                .recordedByEmail(handover.getRecordedByEmail())
                .receivedAt(handover.getReceivedAt())
                .build();
    }

    private String requiredText(
            String value,
            String message
    ) {

        if (value == null ||
                value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private String optionalText(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private Long currentUserId(
            Authentication authentication
    ) {

        if (authentication != null &&
                authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }

        return null;
    }

    private String currentUserName(
            Authentication authentication
    ) {

        if (authentication != null &&
                authentication.getPrincipal() instanceof User user) {
            return user.getName();
        }

        return "SYSTEM";
    }

    private String currentUserEmail(
            Authentication authentication
    ) {

        if (authentication != null &&
                authentication.getPrincipal() instanceof User user) {
            return user.getEmail();
        }

        return "SYSTEM";
    }
}
