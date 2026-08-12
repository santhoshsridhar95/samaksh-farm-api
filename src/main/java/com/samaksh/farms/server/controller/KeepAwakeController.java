package com.samaksh.farms.server.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.server.dto.KeepAwakeSettingsRequest;
import com.samaksh.farms.server.dto.KeepAwakeSettingsResponse;
import com.samaksh.farms.server.service.KeepAwakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/server/keep-awake")
@RequiredArgsConstructor
public class KeepAwakeController {

    private final KeepAwakeService keepAwakeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','FARM_MANAGER')")
    public ApiResponse<KeepAwakeSettingsResponse> settings() {
        return ApiResponse
                .<KeepAwakeSettingsResponse>builder()
                .success(true)
                .message("Keep-awake settings")
                .data(keepAwakeService.settings())
                .build();
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','FARM_MANAGER')")
    public ApiResponse<KeepAwakeSettingsResponse> update(
            @RequestBody KeepAwakeSettingsRequest request
    ) {
        return ApiResponse
                .<KeepAwakeSettingsResponse>builder()
                .success(true)
                .message("Keep-awake settings updated")
                .data(keepAwakeService.update(request))
                .build();
    }

    @PostMapping("/ping")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','FARM_MANAGER')")
    public ApiResponse<KeepAwakeSettingsResponse> pingNow() {
        return ApiResponse
                .<KeepAwakeSettingsResponse>builder()
                .success(true)
                .message("Keep-awake ping executed")
                .data(keepAwakeService.pingNow())
                .build();
    }
}
