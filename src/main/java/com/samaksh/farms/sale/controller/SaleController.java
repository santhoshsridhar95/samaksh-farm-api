package com.samaksh.farms.sale.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.sale.dto.SaleRequest;
import com.samaksh.farms.sale.dto.SaleResponse;
import com.samaksh.farms.sale.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
                .message("Sale created successfully")
                .data(
                        saleService.createSale(
                                request,
                                authentication
                        )
                )
                .build();
    }

    @GetMapping
    public ApiResponse<List<SaleResponse>> getSales() {

        return ApiResponse
                .<List<SaleResponse>>builder()
                .success(true)
                .message("Sales fetched successfully")
                .data(
                        saleService.getSales()
                )
                .build();
    }
}