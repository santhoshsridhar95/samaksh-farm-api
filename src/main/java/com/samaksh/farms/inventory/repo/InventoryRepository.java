package com.samaksh.farms.inventory.repo;

import com.samaksh.farms.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository
        extends JpaRepository<Inventory, Long> {
}