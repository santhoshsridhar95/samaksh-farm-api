package com.samaksh.farms.sourcing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SourcingReceiptRepository extends JpaRepository<SourcingReceipt,Long> { Optional<SourcingReceipt> findByRequestKey(String key); List<SourcingReceipt> findAllByOrderByDateDescIdDesc(); }
