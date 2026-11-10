package com.samaksh.farms.sale.repo;

import com.samaksh.farms.enums.PaymentStatus;
import com.samaksh.farms.sale.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SaleRepository
        extends JpaRepository<Sale, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"customer","product"})
    List<Sale> findByCreatedByUserIdAndSaleDateGreaterThanEqualAndSaleDateLessThan(Long userId,java.time.LocalDateTime from,java.time.LocalDateTime to);

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"customer","product"})
    List<Sale> findAll();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"customer","product"})
    List<Sale> findBySaleDateGreaterThanEqualAndSaleDateLessThan(java.time.LocalDateTime from,java.time.LocalDateTime to);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"customer","product"})
    List<Sale> findByCustomerIdOrderBySaleDateAscIdAsc(Long customerId);

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

    List<Sale> findByCustomerIdAndPaymentStatusInOrderBySaleDateAsc(
            Long customerId,
            Collection<PaymentStatus> paymentStatuses
    );
}
