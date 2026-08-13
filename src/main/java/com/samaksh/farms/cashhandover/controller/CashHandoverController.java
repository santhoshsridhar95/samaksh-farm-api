package com.samaksh.farms.cashhandover.controller;

import com.samaksh.farms.cashhandover.dto.CashHandoverRequest;
import com.samaksh.farms.cashhandover.dto.CashHandoverResponse;
import com.samaksh.farms.cashhandover.dto.CashLedgerResponse;
import com.samaksh.farms.cashhandover.service.CashHandoverService;
import com.samaksh.farms.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cash-handovers")
@RequiredArgsConstructor
public class CashHandoverController {

    private final CashHandoverService cashHandoverService;

    @GetMapping
    public ApiResponse<CashLedgerResponse> getLedger() {

        return ApiResponse
                .<CashLedgerResponse>builder()
                .success(true)
                .message("Cash ledger fetched successfully")
                .data(cashHandoverService.getLedger())
                .build();
    }

    @PostMapping
    public ApiResponse<CashHandoverResponse> createHandover(
            @RequestBody CashHandoverRequest request,
            Authentication authentication
    ) {

        return ApiResponse
                .<CashHandoverResponse>builder()
                .success(true)
                .message("Cash handover recorded successfully")
                .data(cashHandoverService.createHandover(
                        request,
                        authentication
                ))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<CashHandoverResponse> updateHandover(
            @PathVariable Long id,
            @RequestBody CashHandoverRequest request,
            Authentication authentication
    ) {

        return ApiResponse
                .<CashHandoverResponse>builder()
                .success(true)
                .message("Cash handover updated successfully")
                .data(cashHandoverService.updateHandover(
                        id,
                        request,
                        authentication
                ))
                .build();
    }
}
