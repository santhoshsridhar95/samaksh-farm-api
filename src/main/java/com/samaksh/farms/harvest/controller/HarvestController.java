package com.samaksh.farms.harvest.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.harvest.dto.HarvestRequest;
import com.samaksh.farms.harvest.dto.HarvestResponse;
import com.samaksh.farms.harvest.service.HarvestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/harvest")
@RequiredArgsConstructor
public class HarvestController {

    private final HarvestService harvestService;

    @PostMapping
    public ApiResponse<HarvestResponse> createHarvest(
            @RequestBody HarvestRequest request,
            Authentication authentication
    ) {

        return ApiResponse
                .<HarvestResponse>builder()
                .success(true)
                .message("Harvest recorded successfully")
                .data(
                        harvestService.createHarvest(
                                request,
                                authentication
                        )
                )
                .build();
    }

    @GetMapping
    public ApiResponse<List<HarvestResponse>> getHarvests() {

        return ApiResponse
                .<List<HarvestResponse>>builder()
                .success(true)
                .message("Harvests fetched successfully")
                .data(
                        harvestService.getAllHarvests()
                )
                .build();
    }
}