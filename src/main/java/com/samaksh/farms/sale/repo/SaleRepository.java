package com.samaksh.farms.sale.repo;

import com.samaksh.farms.enums.PaymentStatus;
import com.samaksh.farms.sale.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository
        extends JpaRepository<Sale, Long> {

    Page<Sale> findByPaymentStatus(
            PaymentStatus paymentStatus,
            Pageable pageable
    );

    Page<Sale> findByCustomerId(
            Long customerId,
            Pageable pageable
    );

    Page<Sale> findByCustomerIdAndPaymentStatus(
            Long customerId,
            PaymentStatus paymentStatus,
            Pageable pageable
    );
}