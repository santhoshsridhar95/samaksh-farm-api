package com.samaksh.farms.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MoneyEntryRepository extends JpaRepository<MoneyEntry, Long> {
    Optional<MoneyEntry> findByRequestKey(String key);
    boolean existsByReversalOf(Long id);
    List<MoneyEntry> findAllByOrderByDateDescIdDesc();
}
