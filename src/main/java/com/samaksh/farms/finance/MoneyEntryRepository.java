package com.samaksh.farms.finance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MoneyEntryRepository extends JpaRepository<MoneyEntry, Long> {
    List<MoneyEntry> findByEmployeeIdAndDateBetweenOrderByDateDescIdDesc(Long id,java.time.LocalDate from,java.time.LocalDate to);
    List<MoneyEntry> findByEmployeeIdAndSalaryMonthBetween(Long id,String from,String to);
    Optional<MoneyEntry> findByRequestKey(String key);
    boolean existsByReversalOf(Long id);
    List<MoneyEntry> findAllByOrderByDateDescIdDesc();
}
