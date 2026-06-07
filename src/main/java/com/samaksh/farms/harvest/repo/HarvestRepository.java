package com.samaksh.farms.harvest.repo;

import com.samaksh.farms.harvest.entity.Harvest;
import com.samaksh.farms.production.entity.ProductionBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HarvestRepository
        extends JpaRepository<Harvest, Long> {

    List<Harvest> findByBatch(
            ProductionBatch batch
    );
}