package com.samaksh.farms.production.controller;

import com.samaksh.farms.production.dto.ProductionBatchRequest;
import com.samaksh.farms.production.dto.ProductionBatchResponse;
import com.samaksh.farms.production.service.ProductionBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production")
@RequiredArgsConstructor
public class ProductionBatchController {

    private final ProductionBatchService productionBatchService;

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','FARM_MANAGER')"
    )
    public ProductionBatchResponse createBatch(
            @RequestBody
            ProductionBatchRequest request
    ) {

        return productionBatchService
                .createBatch(request);
    }

    @GetMapping
    public List<ProductionBatchResponse> getBatches() {

        return productionBatchService
                .getAllBatches();
    }
}