package com.samaksh.farms.inventorytransaction.repo;

import com.samaksh.farms.enums.InventoryType;
import com.samaksh.farms.inventorytransaction.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction>
    findByInventoryType(
            String inventoryType
    );
    default List<InventoryTransaction> findByInventoryType(InventoryType type) { return findByInventoryType(type.name()); }
}
