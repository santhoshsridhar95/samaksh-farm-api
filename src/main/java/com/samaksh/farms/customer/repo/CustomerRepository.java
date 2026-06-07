package com.samaksh.farms.customer.repo;

import com.samaksh.farms.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository
        extends JpaRepository<Customer, Long> {
}