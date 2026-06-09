package com.samaksh.farms.productionmovement.repo;

import com.samaksh.farms.productionmovement.entity.ProductionMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionMovementRepository
        extends JpaRepository<ProductionMovement, Long> {
}