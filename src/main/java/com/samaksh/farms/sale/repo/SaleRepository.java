package com.samaksh.farms.sale.repo;

import com.samaksh.farms.sale.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository
        extends JpaRepository<Sale, Long> {
}