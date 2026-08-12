package com.samaksh.farms.entitlement.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.entitlement.dto.EntitlementResponse;
import com.samaksh.farms.entitlement.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/entitlements")
@RequiredArgsConstructor
public class EntitlementController {

    private final EntitlementService entitlementService;

    @GetMapping("/me")
    public ApiResponse<EntitlementResponse> getMine(
            Authentication authentication
    ) {

        return ApiResponse
                .<EntitlementResponse>builder()
                .success(true)
                .message("Entitlements fetched successfully")
                .data(
                        entitlementService.getCurrentUserEntitlements(
                                authentication
                        )
                )
                .build();
    }
}
