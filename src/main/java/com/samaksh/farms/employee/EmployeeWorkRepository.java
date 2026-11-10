package com.samaksh.farms.employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.LocalDate;
public interface EmployeeWorkRepository extends JpaRepository<EmployeeWork,Long> {
 Optional<EmployeeWork> findByRequestKey(String key);
 List<EmployeeWork> findByEmployeeIdAndDateBetweenOrderByDateDescIdDesc(Long id,LocalDate from,LocalDate to);
}
