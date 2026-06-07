package com.samaksh.farms.finishedgoods.repo;

import com.samaksh.farms.finishedgoods.entity.FinishedGoodsInventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinishedGoodsInventoryRepository
        extends JpaRepository<FinishedGoodsInventory, Long> {
}