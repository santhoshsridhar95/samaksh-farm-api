package com.samaksh.farms.analytics.service;

import com.samaksh.farms.analytics.dto.YieldAnalyticsResponse;
import com.samaksh.farms.harvest.entity.Harvest;
import com.samaksh.farms.harvest.repo.HarvestRepository;
import com.samaksh.farms.production.entity.ProductionBatch;
import com.samaksh.farms.production.repo.ProductionBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ProductionBatchRepository batchRepository;

    private final HarvestRepository harvestRepository;

    public List<YieldAnalyticsResponse> getYieldAnalytics() {

        return batchRepository.findAll()
                .stream()
                .map(this::calculateYield)
                .toList();
    }

    private YieldAnalyticsResponse calculateYield(
            ProductionBatch batch
    ) {

        List<Harvest> harvests =
                harvestRepository.findByBatch(batch);

        double totalHarvest =
                harvests.stream()
                        .mapToDouble(
                                Harvest::getQuantity
                        )
                        .sum();

        double yieldPerSpawn = 0;

        if (batch.getSpawnUsed() != null
                && batch.getSpawnUsed() > 0) {

            yieldPerSpawn =
                    totalHarvest
                            / batch.getSpawnUsed();
        }

        return YieldAnalyticsResponse.builder()
                .batchCode(
                        batch.getBatchCode()
                )
                .spawnUsed(
                        batch.getSpawnUsed()
                )
                .totalHarvestKg(
                        totalHarvest
                )
                .yieldPerSpawn(
                        yieldPerSpawn
                )
                .build();
    }
}