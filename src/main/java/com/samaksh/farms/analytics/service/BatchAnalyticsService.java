package com.samaksh.farms.analytics.service;

import com.samaksh.farms.analytics.dto.BatchAnalyticsResponse;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.harvest.entity.Harvest;
import com.samaksh.farms.harvest.repo.HarvestRepository;
import com.samaksh.farms.production.entity.ProductionBatch;
import com.samaksh.farms.production.repo.ProductionBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchAnalyticsService {

    private final ProductionBatchRepository batchRepository;

    private final HarvestRepository harvestRepository;

    public BatchAnalyticsResponse getBatchAnalytics(
            Long batchId
    ) {

        ProductionBatch batch =
                batchRepository.findById(
                        batchId
                ).orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Batch",
                                batchId
                        )
                );

        List<Harvest> harvests =
                harvestRepository.findByBatch(
                        batch
                );

        double totalHarvestKg =
                harvests.stream()
                        .mapToDouble(
                                Harvest::getQuantity
                        )
                        .sum();

        double yieldPerBag =
                batch.getBagsPrepared() == null
                        || batch.getBagsPrepared() == 0
                        ? 0
                        : totalHarvestKg
                        / batch.getBagsPrepared();

        double yieldPerSpawnKg =
                batch.getSpawnUsedKg() == null
                        || batch.getSpawnUsedKg() == 0
                        ? 0
                        : totalHarvestKg
                        / batch.getSpawnUsedKg();

        return BatchAnalyticsResponse.builder()
                .batchCode(
                        batch.getBatchCode()
                )
                .bagsPrepared(
                        batch.getBagsPrepared()
                )
                .spawnUsedKg(
                        batch.getSpawnUsedKg()
                )
                .pelletsUsedKg(
                        batch.getPelletsUsedKg()
                )
                .totalHarvestKg(
                        totalHarvestKg
                )
                .yieldPerBag(
                        yieldPerBag
                )
                .yieldPerSpawnKg(
                        yieldPerSpawnKg
                )
                .contaminatedBags(
                        batch.getContaminatedBags()
                )
                .discardedBags(
                        batch.getDiscardedBags()
                )
                .build();
    }
}