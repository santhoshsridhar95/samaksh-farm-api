package com.samaksh.farms.employee;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Table(name="employees") @Getter @Setter @NoArgsConstructor
public class Employee {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true) private String requestKey;
 private String createFingerprint;
 @Column(nullable=false,length=150) private String name;
 @Column(nullable=false,length=150) private String designation;
 @Column(length=300) private String responsibilities;
 @Column(unique=true) private Long userId;
 private LocalDate joinedOn;
 private LocalDate leftOn;
 private boolean active;
 @Version private Long version;
 private LocalDateTime recordedAt;
 private String recordedBy;
 private LocalDateTime updatedAt;
 private String updatedBy;
}
