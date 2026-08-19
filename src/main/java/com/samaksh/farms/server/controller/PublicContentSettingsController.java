package com.samaksh.farms.server.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.server.dto.PublicContentSettingsRequest;
import com.samaksh.farms.server.dto.PublicContentSettingsResponse;
import com.samaksh.farms.server.service.PublicContentSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PublicContentSettingsController {

    private final PublicContentSettingsService publicContentSettingsService;

    @GetMapping("/api/public-content/settings")
    public ApiResponse<PublicContentSettingsResponse> publicSettings() {
        return ApiResponse
                .<PublicContentSettingsResponse>builder()
                .success(true)
                .message("Public content settings")
                .data(publicContentSettingsService.settings())
                .build();
    }

    @PutMapping("/api/server/public-content")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','FARM_MANAGER')")
    public ApiResponse<PublicContentSettingsResponse> update(
            @RequestBody PublicContentSettingsRequest request
    ) {
        return ApiResponse
                .<PublicContentSettingsResponse>builder()
                .success(true)
                .message("Public content settings updated")
                .data(publicContentSettingsService.update(request))
                .build();
    }
}
