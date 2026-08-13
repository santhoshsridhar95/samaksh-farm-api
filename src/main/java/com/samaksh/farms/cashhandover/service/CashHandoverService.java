package com.samaksh.farms.cashhandover.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.cashhandover.dto.CashCollectionSummaryResponse;
import com.samaksh.farms.cashhandover.dto.CashHandoverRequest;
import com.samaksh.farms.cashhandover.dto.CashHandoverResponse;
import com.samaksh.farms.cashhandover.dto.CashLedgerResponse;
import com.samaksh.farms.cashhandover.entity.CashHandover;
import com.samaksh.farms.cashhandover.repo.CashHandoverRepository;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.common.time.BusinessTime;
import com.samaksh.farms.sale.entity.Sale;
import com.samaksh.farms.sale.repo.SaleRepository;
import com.samaksh.farms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CashHandoverService {

    private final CashHandoverRepository cashHandoverRepository;

    private final SaleRepository saleRepository;

    private final AuditService auditService;

    public CashLedgerResponse getLedger() {

        List<CashHandover> handovers =
                cashHandoverRepository.findAllByOrderByHandedOverAtDesc();

        return CashLedgerResponse.builder()
                .summaries(buildSummaries(handovers))
                .handovers(
                        handovers.stream()
                                .map(this::mapToResponse)
                                .toList()
                )
                .build();
    }

    public CashHandoverResponse createHandover(
            CashHandoverRequest request,
            Authentication authentication
    ) {

        validateRequest(request);

        CashHandover handover =
                CashHandover.builder()
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
                        .amount(request.getAmount())
                        .remarks(optionalText(request.getRemarks()))
                        .recordedByUserId(currentUserId(authentication))
                        .recordedByName(currentUserName(authentication))
                        .recordedByEmail(currentUserEmail(authentication))
                        .handedOverAt(BusinessTime.now())
                        .build();

        CashHandover saved =
                cashHandoverRepository.save(handover);

        auditService.createAudit(
                authentication,
                "CASH_HANDOVER",
                "CREATE_CASH_HANDOVER",
                saved.getCollectorName(),
                "Cash handed over to " + saved.getOwnerName()
                        + ": Rs. " + saved.getAmount()
        );

        return mapToResponse(saved);
    }

    public CashHandoverResponse updateHandover(
            Long id,
            CashHandoverRequest request,
            Authentication authentication
    ) {

        validateRequest(request);

        CashHandover handover =
                cashHandoverRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Cash handover",
                                id
                        ));

        handover.setCollectorUserId(request.getCollectorUserId());
        handover.setCollectorName(requiredText(
                request.getCollectorName(),
                "Collector name is required"
        ));
        handover.setCollectorEmail(optionalText(request.getCollectorEmail()));
        handover.setOwnerName(requiredText(
                request.getOwnerName(),
                "Owner name is required"
        ));
        handover.setAmount(request.getAmount());
        handover.setRemarks(optionalText(request.getRemarks()));
        handover.setUpdatedAt(BusinessTime.now());

        CashHandover saved =
                cashHandoverRepository.save(handover);

        auditService.createAudit(
                authentication,
                "CASH_HANDOVER",
                "UPDATE_CASH_HANDOVER",
                saved.getCollectorName(),
                "Cash handover updated for " + saved.getOwnerName()
                        + ": Rs. " + saved.getAmount()
        );

        return mapToResponse(saved);
    }

    private List<CashCollectionSummaryResponse> buildSummaries(
            List<CashHandover> handovers
    ) {

        Map<String, CashTotals> totalsByCollector =
                new LinkedHashMap<>();
        LocalDate today =
                BusinessTime.today();

        for (Sale sale : saleRepository.findAll()) {
            double collected =
                    sale.getAmountCollected() == null
                            ? 0
                            : sale.getAmountCollected();

            if (collected <= 0) {
                continue;
            }

            CashTotals totals =
                    totalsByCollector.computeIfAbsent(
                            collectorKey(
                                    sale.getCreatedByUserId(),
                                    sale.getCreatedByEmail(),
                                    sale.getCreatedByName()
                            ),
                            ignored -> new CashTotals(
                                    sale.getCreatedByUserId(),
                                    sale.getCreatedByName(),
                                    sale.getCreatedByEmail()
                            )
                    );

            totals.totalCollected += collected;

            if (sale.getSaleDate() != null &&
                    sale.getSaleDate().toLocalDate().equals(today)) {
                totals.todayCollected += collected;
            }
        }

        for (CashHandover handover : handovers) {
            CashTotals totals =
                    totalsByCollector.computeIfAbsent(
                            collectorKey(
                                    handover.getCollectorUserId(),
                                    handover.getCollectorEmail(),
                                    handover.getCollectorName()
                            ),
                            ignored -> new CashTotals(
                                    handover.getCollectorUserId(),
                                    handover.getCollectorName(),
                                    handover.getCollectorEmail()
                            )
                    );

            totals.totalHandedOver += handover.getAmount() == null
                    ? 0
                    : handover.getAmount();
        }

        return totalsByCollector.values()
                .stream()
                .sorted(
                        Comparator.comparingDouble(
                                CashTotals::balanceWithUser
                        ).reversed()
                )
                .map(totals -> CashCollectionSummaryResponse.builder()
                        .collectorUserId(totals.collectorUserId)
                        .collectorName(totals.collectorName)
                        .collectorEmail(totals.collectorEmail)
                        .todayCollected(totals.todayCollected)
                        .totalCollected(totals.totalCollected)
                        .totalHandedOver(totals.totalHandedOver)
                        .balanceWithUser(totals.balanceWithUser())
                        .build()
                )
                .toList();
    }

    private void validateRequest(
            CashHandoverRequest request
    ) {

        if (request.getAmount() == null ||
                request.getAmount() <= 0) {
            throw new IllegalArgumentException(
                    "Handover amount must be greater than 0"
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

    private CashHandoverResponse mapToResponse(
            CashHandover handover
    ) {

        return CashHandoverResponse.builder()
                .id(handover.getId())
                .collectorUserId(handover.getCollectorUserId())
                .collectorName(handover.getCollectorName())
                .collectorEmail(handover.getCollectorEmail())
                .ownerName(handover.getOwnerName())
                .amount(handover.getAmount())
                .remarks(handover.getRemarks())
                .recordedByName(handover.getRecordedByName())
                .recordedByEmail(handover.getRecordedByEmail())
                .handedOverAt(handover.getHandedOverAt())
                .updatedAt(handover.getUpdatedAt())
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

    private String collectorKey(
            Long userId,
            String email,
            String name
    ) {

        if (userId != null) {
            return "id:" + userId;
        }

        if (email != null &&
                !email.isBlank()) {
            return "email:" + email.toLowerCase();
        }

        return "name:" + String.valueOf(name).toLowerCase();
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

    private static class CashTotals {

        private final Long collectorUserId;

        private final String collectorName;

        private final String collectorEmail;

        private double todayCollected;

        private double totalCollected;

        private double totalHandedOver;

        private CashTotals(
                Long collectorUserId,
                String collectorName,
                String collectorEmail
        ) {

            this.collectorUserId = collectorUserId;
            this.collectorName = collectorName == null ||
                    collectorName.isBlank()
                    ? "Unknown user"
                    : collectorName;
            this.collectorEmail = collectorEmail;
        }

        private double balanceWithUser() {
            return Math.max(
                    0,
                    totalCollected - totalHandedOver
            );
        }
    }
}
