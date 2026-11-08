package com.samaksh.farms.sourcing;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name="sourcing_payments") @Getter @Setter @NoArgsConstructor
public class SourcingPayment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(unique=true, nullable=false) private String requestKey;
    private String requestFingerprint;
    private String lastMutationKey;
    private Long receiptId;
    private Long sourceAccountId;
    private Long moneyEntryId;
    @Column(precision=18,scale=2) private BigDecimal amount;
    @Column(precision=18,scale=2) private BigDecimal additionalAmount;
    private Long advanceEntryId;
    private LocalDate date;
    private String notes;
}
