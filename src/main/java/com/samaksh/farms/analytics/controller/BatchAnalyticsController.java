package com.samaksh.farms.analytics.controller;

import com.samaksh.farms.analytics.dto.BatchAnalyticsResponse;
import com.samaksh.farms.analytics.service.BatchAnalyticsService;
import com.samaksh.farms.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/batch-analytics")
@RequiredArgsConstructor
public class BatchAnalyticsController {

    private final BatchAnalyticsService
            analyticsService;

    @GetMapping("/{batchId}")
    public ApiResponse<BatchAnalyticsResponse>
    getAnalytics(
            @PathVariable Long batchId
    ) {

        return ApiResponse
                .<BatchAnalyticsResponse>builder()
                .success(true)
                .message(
                        "Batch analytics fetched successfully"
                )
                .data(
                        analyticsService
                                .getBatchAnalytics(
                                        batchId
                                )
                )
                .build();
    }
}