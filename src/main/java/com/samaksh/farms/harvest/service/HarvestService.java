package com.samaksh.farms.harvest.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.harvest.dto.HarvestRequest;
import com.samaksh.farms.harvest.dto.HarvestResponse;
import com.samaksh.farms.harvest.entity.Harvest;
import com.samaksh.farms.harvest.repo.HarvestRepository;
import com.samaksh.farms.production.entity.ProductionBatch;
import com.samaksh.farms.production.repo.ProductionBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HarvestService {

    private final HarvestRepository harvestRepository;

    private final ProductionBatchRepository batchRepository;

    private final AuditService auditService;

    public HarvestResponse createHarvest(
            HarvestRequest request,
            Authentication authentication
    ) {

        ProductionBatch batch =
                batchRepository.findById(
                        request.getBatchId()
                ).orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Batch",
                                        request.getBatchId()
                                )
                );

        Harvest harvest =
                Harvest.builder()
                        .batch(batch)
                        .quantity(
                                request.getQuantity()
                        )
                        .remarks(
                                request.getRemarks()
                        )
                        .harvestDate(
                                LocalDate.now()
                        )
                        .build();

        Harvest saved =
                harvestRepository.save(
                        harvest
                );

        auditService.createAudit(
                authentication,
                "HARVEST",
                "CREATE_HARVEST",
                batch.getBatchCode(),
                "Harvest recorded : "
                        + request.getQuantity()
                        + " KG"
        );

        return mapToResponse(saved);
    }

    public List<HarvestResponse> getAllHarvests() {

        return harvestRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private HarvestResponse mapToResponse(
            Harvest harvest
    ) {

        return HarvestResponse.builder()
                .id(harvest.getId())
                .batchCode(
                        harvest.getBatch()
                                .getBatchCode()
                )
                .quantity(
                        harvest.getQuantity()
                )
                .remarks(
                        harvest.getRemarks()
                )
                .build();
    }
}