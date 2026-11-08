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
    private final com.samaksh.farms.analytics.service.ShopPriceService prices;
    @GetMapping("/shop-prices")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('SUPER_ADMIN','FARM_MANAGER')")
    public ApiResponse<?> shopPrices(@org.springframework.web.bind.annotation.RequestParam(required=false) java.time.LocalDate from,@org.springframework.web.bind.annotation.RequestParam(required=false) java.time.LocalDate to){return ApiResponse.builder().success(true).data(prices.report(from,to)).build();}

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