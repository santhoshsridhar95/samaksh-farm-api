package com.samaksh.farms.order.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.order.dto.CustomerOrderRequest;
import com.samaksh.farms.order.dto.CustomerOrderResponse;
import com.samaksh.farms.order.service.CustomerOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final CustomerOrderService orderService;

    @PostMapping
    public ApiResponse<CustomerOrderResponse>
    createOrder(
            @RequestBody CustomerOrderRequest request,
            Authentication authentication
    ) {

        return ApiResponse
                .<CustomerOrderResponse>builder()
                .success(true)
                .message("Order created successfully")
                .data(
                        orderService.createOrder(
                                request,
                                authentication
                        )
                )
                .build();
    }

    @PutMapping("/{id}/fulfill")
    public ApiResponse<CustomerOrderResponse>
    fulfillOrder(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return ApiResponse
                .<CustomerOrderResponse>builder()
                .success(true)
                .message("Order fulfilled successfully")
                .data(
                        orderService.fulfillOrder(
                                id,
                                authentication
                        )
                )
                .build();
    }

    @GetMapping
    public ApiResponse<List<CustomerOrderResponse>>
    getOrders() {

        return ApiResponse
                .<List<CustomerOrderResponse>>builder()
                .success(true)
                .message("Orders fetched successfully")
                .data(
                        orderService.getOrders()
                )
                .build();
    }
}