package com.samaksh.farms.employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface EmployeeRepository extends JpaRepository<Employee,Long> {
 Optional<Employee> findByRequestKey(String key);
 List<Employee> findAllByOrderByNameAsc();
 boolean existsByUserIdAndIdNot(Long userId,Long id);
}
