package com.samaksh.farms.sourcing;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name="sourcing_recoveries") @Getter @Setter @NoArgsConstructor
public class SourcingRecovery {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(unique=true,nullable=false) private String requestKey;
 private String requestFingerprint;
 private String lastMutationKey;
 private Long paymentId;
 private Long destinationAccountId;
 private Long moneyEntryId;
 @Column(precision=18,scale=2) private BigDecimal amount;
 private LocalDate date;
 private String notes;
}
