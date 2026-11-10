package com.samaksh.farms.employee;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="employee_work",indexes=@Index(columnList="employee_id,date")) @Getter @Setter @NoArgsConstructor
public class EmployeeWork {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true) private String requestKey;
 private String fingerprint;
 @Column(nullable=false) private Long employeeId;
 private LocalDate date;
 private String category;
 @Column(precision=19,scale=2) private BigDecimal quantity;
 @Column(length=1000) private String notes;
 private boolean voided;
 private LocalDateTime recordedAt;
 private String recordedBy;
}
