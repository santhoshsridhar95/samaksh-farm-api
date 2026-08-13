package com.samaksh.farms.exchangebox.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.exchangebox.dto.ExchangeBoxHandoverRequest;
import com.samaksh.farms.exchangebox.dto.ExchangeBoxHandoverResponse;
import com.samaksh.farms.exchangebox.service.ExchangeBoxHandoverService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/exchange-box-handovers")
@RequiredArgsConstructor
public class ExchangeBoxHandoverController {

    private final ExchangeBoxHandoverService service;

    @GetMapping
    public ApiResponse<List<ExchangeBoxHandoverResponse>> getHandovers() {

        return ApiResponse
                .<List<ExchangeBoxHandoverResponse>>builder()
                .success(true)
                .message("Exchange box handovers fetched successfully")
                .data(service.getHandovers())
                .build();
    }

    @PostMapping
    public ApiResponse<ExchangeBoxHandoverResponse> createHandover(
            @RequestBody ExchangeBoxHandoverRequest request,
            Authentication authentication
    ) {

        return ApiResponse
                .<ExchangeBoxHandoverResponse>builder()
                .success(true)
                .message("Exchange boxes received successfully")
                .data(service.createHandover(
                        request,
                        authentication
                ))
                .build();
    }
}
