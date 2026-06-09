package com.samaksh.farms.analytics.controller;

import com.samaksh.farms.analytics.dto.FarmDashboardResponse;
import com.samaksh.farms.analytics.service.FarmDashboardService;
import com.samaksh.farms.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/farm-dashboard")
@RequiredArgsConstructor
public class FarmDashboardController {

    private final FarmDashboardService dashboardService;

    @GetMapping
    public ApiResponse<FarmDashboardResponse>
    getDashboard() {

        return ApiResponse
                .<FarmDashboardResponse>builder()
                .success(true)
                .message(
                        "Farm dashboard fetched successfully"
                )
                .data(
                        dashboardService.getDashboard()
                )
                .build();
    }
}