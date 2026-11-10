package com.samaksh.farms.employee;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="employee_salaries",uniqueConstraints=@UniqueConstraint(columnNames={"employee_id","effective_month"})) @Getter @Setter @NoArgsConstructor
public class EmployeeSalary {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long employeeId;
 @Column(nullable=false) private String effectiveMonth;
 @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
 private LocalDateTime updatedAt;
 private String updatedBy;
}
