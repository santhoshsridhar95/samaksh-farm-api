package com.samaksh.farms.employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface EmployeeSalaryRepository extends JpaRepository<EmployeeSalary,Long> {
 List<EmployeeSalary> findByEmployeeIdOrderByEffectiveMonthDesc(Long id);
 Optional<EmployeeSalary> findByEmployeeIdAndEffectiveMonth(Long id,String month);
}
