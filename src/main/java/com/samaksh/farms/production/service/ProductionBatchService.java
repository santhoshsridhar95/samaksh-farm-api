package com.samaksh.farms.production.service;

import com.samaksh.farms.enums.BatchStatus;
import com.samaksh.farms.enums.MushroomType;
import com.samaksh.farms.production.dto.ProductionBatchRequest;
import com.samaksh.farms.production.dto.ProductionBatchResponse;
import com.samaksh.farms.production.entity.ProductionBatch;
import com.samaksh.farms.production.repo.ProductionBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductionBatchService {

    private final ProductionBatchRepository productionBatchRepository;

    public ProductionBatchResponse createBatch(
            ProductionBatchRequest request
    ) {

        ProductionBatch batch =
                ProductionBatch.builder()
                        .batchCode(
                                generateBatchCode(request.getMushroomType())
                        )
                        .spawnUsed(
                                request.getSpawnUsed()
                        )
                        .pelletsUsed(
                                request.getPelletsUsed()
                        )
                        .bagsUsed(
                                request.getBagsUsed()
                        )
                        .startDate(
                                LocalDate.now()
                        )
                        .status(
                                BatchStatus.ACTIVE
                        )
                        .build();

        ProductionBatch saved =
                productionBatchRepository.save(batch);

        return mapToResponse(saved);
    }

    public List<ProductionBatchResponse> getAllBatches() {

        return productionBatchRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ProductionBatchResponse mapToResponse(
            ProductionBatch batch
    ) {

        return ProductionBatchResponse.builder()
                .id(batch.getId())
                .batchCode(batch.getBatchCode())
                .spawnUsed(batch.getSpawnUsed())
                .pelletsUsed(batch.getPelletsUsed())
                .bagsUsed(batch.getBagsUsed())
                .status(batch.getStatus())
                .build();
    }

    private String generateBatchCode(
            MushroomType type
    ) {

        String prefix =
                switch (type) {

                    case OYSTER -> "OYS";

                    case BUTTON -> "BTN";

                    case MILKY -> "MLK";
                };

        String date =
                LocalDate.now()
                        .toString()
                        .replace("-", "");

        long count =
                productionBatchRepository.count() + 1;

        return prefix +
                "-" +
                date +
                "-" +
                String.format(
                        "%03d",
                        count
                );
    }
}