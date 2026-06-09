package com.samaksh.farms.order.repo;

import com.samaksh.farms.enums.OrderStatus;
import com.samaksh.farms.order.entity.CustomerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    Page<CustomerOrder> findByStatus(
            OrderStatus status,
            Pageable pageable
    );
}