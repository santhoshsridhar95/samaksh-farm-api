package com.samaksh.farms.sale.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.enums.PaymentStatus;
import com.samaksh.farms.sale.dto.PagedResponse;
import com.samaksh.farms.sale.dto.SaleRequest;
import com.samaksh.farms.sale.dto.SaleResponse;
import com.samaksh.farms.sale.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @PostMapping
    public ApiResponse<SaleResponse> createSale(
            @RequestBody SaleRequest request,
            Authentication authentication
    ) {

        return ApiResponse
                .<SaleResponse>builder()
                .success(true)
                .message(
                        "Sale created successfully"
                )
                .data(
                        saleService.createSale(
                                request,
                                authentication
                        )
                )
                .build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<SaleResponse>>
    getSales(

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "10"
            )
            int size,

            @RequestParam(
                    required = false
            )
            Long customerId,

            @RequestParam(
                    required = false
            )
            PaymentStatus paymentStatus
    ) {

        return ApiResponse
                .<PagedResponse<SaleResponse>>builder()
                .success(true)
                .message(
                        "Sales fetched successfully"
                )
                .data(
                        saleService.getSales(
                                page,
                                size,
                                customerId,
                                paymentStatus
                        )
                )
                .build();
    }
}