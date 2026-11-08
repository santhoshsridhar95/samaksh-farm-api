package com.samaksh.farms.sourcing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SourcingPaymentRepository extends JpaRepository<SourcingPayment,Long> { Optional<SourcingPayment> findByRequestKey(String key); List<SourcingPayment> findByReceiptId(Long id); }
