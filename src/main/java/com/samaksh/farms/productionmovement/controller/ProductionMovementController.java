package com.samaksh.farms.productionmovement.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.productionmovement.dto.ProductionMovementResponse;
import com.samaksh.farms.productionmovement.service.ProductionMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production-movements")
@RequiredArgsConstructor
public class ProductionMovementController {

    private final ProductionMovementService
            productionMovementService;

    @GetMapping
    public ApiResponse<
            List<ProductionMovementResponse>>
    getMovements() {

        return ApiResponse
                .<List<ProductionMovementResponse>>
                        builder()
                .success(true)
                .message(
                        "Production movements fetched successfully"
                )
                .data(
                        productionMovementService
                                .getAllMovements()
                )
                .build();
    }
}