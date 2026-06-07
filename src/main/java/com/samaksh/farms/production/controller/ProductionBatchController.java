package com.samaksh.farms.production.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.production.dto.ProductionBatchRequest;
import com.samaksh.farms.production.dto.ProductionBatchResponse;
import com.samaksh.farms.production.service.ProductionBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production")
@RequiredArgsConstructor
public class ProductionBatchController {

    private final ProductionBatchService productionBatchService;

    @PostMapping
    public ApiResponse<ProductionBatchResponse> createBatch(
            @RequestBody ProductionBatchRequest request,
            Authentication authentication
    ) {

        return ApiResponse
                .<ProductionBatchResponse>builder()
                .success(true)
                .message("Production batch created successfully")
                .data(
                        productionBatchService.createBatch(
                                request,
                                authentication
                        )
                )
                .build();
    }

    @GetMapping
    public ApiResponse<List<ProductionBatchResponse>> getBatches() {

        return ApiResponse
                .<List<ProductionBatchResponse>>builder()
                .success(true)
                .message("Production batches fetched successfully")
                .data(
                        productionBatchService.getAllBatches()
                )
                .build();
    }
}