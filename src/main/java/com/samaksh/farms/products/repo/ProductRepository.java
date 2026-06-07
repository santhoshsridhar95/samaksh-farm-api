package com.samaksh.farms.products.repo;

import com.samaksh.farms.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository
        extends JpaRepository<Product, Long> {
}