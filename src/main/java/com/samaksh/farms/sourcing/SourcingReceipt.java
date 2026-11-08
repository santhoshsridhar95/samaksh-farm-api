package com.samaksh.farms.sourcing;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name="sourcing_receipts") @Getter @Setter @NoArgsConstructor
public class SourcingReceipt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(unique=true, nullable=false) private String requestKey;
    private String requestFingerprint;
    private String lastMutationKey;
    private Long vendorId;
    private LocalDate date;
    private String product;
    @Column(precision=18,scale=6) private BigDecimal quantityKg;
    @Column(precision=18,scale=6) private BigDecimal boxes;
    @Column(precision=15,scale=4) private BigDecimal boxesPerKg;
    @Column(precision=15,scale=4) private BigDecimal costPerKg;
    @Column(precision=15,scale=4) private BigDecimal sellingPricePerKg;
    @Column(precision=18,scale=2) private BigDecimal transportCost;
    @Column(precision=18,scale=2) private BigDecimal otherCost;
    private String expensesBorneBy;
    private String expensesPaidBy;
    private Long expenseAccountId;
    private Long expenseEntryId;
    @Column(precision=18,scale=6) private BigDecimal soldKg;
    @Column(precision=18,scale=3) private BigDecimal damagedBoxes;
    @Column(length=1000) private String notes;
}
