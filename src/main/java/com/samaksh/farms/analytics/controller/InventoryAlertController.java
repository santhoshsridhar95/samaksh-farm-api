package com.samaksh.farms.analytics.controller;

import com.samaksh.farms.analytics.dto.InventoryAlertResponse;
import com.samaksh.farms.analytics.service.InventoryAlertService;
import com.samaksh.farms.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-alerts")
@RequiredArgsConstructor
public class InventoryAlertController {

    private final InventoryAlertService service;

    @GetMapping
    public ApiResponse<List<InventoryAlertResponse>>
    getAlerts() {

        return ApiResponse
                .<List<InventoryAlertResponse>>builder()
                .success(true)
                .message(
                        "Inventory alerts fetched successfully"
                )
                .data(
                        service.getAlerts()
                )
                .build();
    }
}