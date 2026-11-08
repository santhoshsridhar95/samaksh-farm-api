package com.samaksh.farms.sourcing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SourcingRecoveryRepository extends JpaRepository<SourcingRecovery,Long> {
 Optional<SourcingRecovery> findByRequestKey(String key);
 List<SourcingRecovery> findByPaymentId(Long id);
}
