package com.samaksh.farms.analytics.controller;

import com.samaksh.farms.analytics.dto.YieldAnalyticsResponse;
import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/yield")
    public ApiResponse<List<YieldAnalyticsResponse>>
    getYieldAnalytics() {

        return ApiResponse
                .<List<YieldAnalyticsResponse>>builder()
                .success(true)
                .message("Yield analytics fetched successfully")
                .data(
                        analyticsService.getYieldAnalytics()
                )
                .build();
    }
}