package com.samaksh.farms.customer.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.customer.dto.CustomerRequest;
import com.samaksh.farms.customer.dto.CustomerResponse;
import com.samaksh.farms.customer.dto.PagedResponse;
import com.samaksh.farms.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ApiResponse<CustomerResponse> createCustomer(
            @RequestBody CustomerRequest request,
            Authentication authentication
    ) {

        return ApiResponse
                .<CustomerResponse>builder()
                .success(true)
                .message(
                        "Customer created successfully"
                )
                .data(
                        customerService.createCustomer(
                                request,
                                authentication
                        )
                )
                .build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<CustomerResponse>>
    getCustomers(

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
            String search
    ) {

        return ApiResponse
                .<PagedResponse<CustomerResponse>>builder()
                .success(true)
                .message(
                        "Customers fetched successfully"
                )
                .data(
                        customerService.getCustomers(
                                page,
                                size,
                                search
                        )
                )
                .build();
    }
}