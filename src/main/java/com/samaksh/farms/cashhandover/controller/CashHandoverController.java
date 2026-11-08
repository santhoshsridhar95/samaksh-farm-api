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
    public ApiResponse<?> createHandover(@RequestBody CashHandoverRequest request) {
        throw new IllegalArgumentException("Use Money & Expenses to transfer funds between named accounts.");
    }
    @PutMapping("/{id}")
    public ApiResponse<?> updateHandover(@PathVariable Long id, @RequestBody CashHandoverRequest request) {
        throw new IllegalArgumentException("Historical handovers are retained for audit. Use Money & Expenses for corrections.");
    }
}
