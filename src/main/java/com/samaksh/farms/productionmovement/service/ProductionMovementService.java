package com.samaksh.farms.productionmovement.service;

import com.samaksh.farms.productionmovement.dto.ProductionMovementResponse;
import com.samaksh.farms.productionmovement.entity.ProductionMovement;
import com.samaksh.farms.productionmovement.repo.ProductionMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductionMovementService {

    private final ProductionMovementRepository
            productionMovementRepository;

    public List<ProductionMovementResponse>
    getAllMovements() {

        return productionMovementRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ProductionMovementResponse
    mapToResponse(
            ProductionMovement movement
    ) {

        return ProductionMovementResponse.builder()
                .id(
                        movement.getId()
                )
                .batchCode(
                        movement.getBatch()
                                .getBatchCode()
                )
                .movedToLightRoom(
                        movement.getMovedToLightRoom()
                )
                .contaminatedBags(
                        movement.getContaminatedBags()
                )
                .discardedBags(
                        movement.getDiscardedBags()
                )
                .remarks(
                        movement.getRemarks()
                )
                .movementDate(
                        movement.getMovementDate()
                )
                .build();
    }
}