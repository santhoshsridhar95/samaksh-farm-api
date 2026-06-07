package com.samaksh.farms.dashboard.service;

import com.samaksh.farms.dashboard.dto.DashboardResponse;
import com.samaksh.farms.enums.BatchStatus;
import com.samaksh.farms.harvest.repo.HarvestRepository;
import com.samaksh.farms.production.repo.ProductionBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductionBatchRepository batchRepository;

    private final HarvestRepository harvestRepository;

    private final InventoryBalanceService inventoryBalanceService;

    public DashboardResponse getDashboard() {

        long activeBatches =
                batchRepository.findAll()
                        .stream()
                        .filter(batch ->
                                batch.getStatus() ==
                                        BatchStatus.ACTIVE)
                        .count();

        long harvestCount =
                harvestRepository.count();

        return DashboardResponse.builder()
                .activeBatches(
                        activeBatches
                )
                .totalHarvestEntries(
                        harvestCount
                )
                .inventory(
                        inventoryBalanceService
                                .getInventoryBalance()
                )
                .build();
    }
}