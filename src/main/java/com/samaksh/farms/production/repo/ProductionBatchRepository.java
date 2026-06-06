package com.samaksh.farms.production.repo;

import com.samaksh.farms.production.entity.ProductionBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionBatchRepository
        extends JpaRepository<ProductionBatch, Long> {
}