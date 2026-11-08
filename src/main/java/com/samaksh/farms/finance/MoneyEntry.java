package com.samaksh.farms.finance;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name = "money_entries")
@Getter @Setter @NoArgsConstructor
public class MoneyEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String requestKey;
    private String requestFingerprint;
    @Column(nullable = false) private String type;
    private Long sourceId;
    private Long destinationId;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(length = 1000) private String description;
    private String payee;
    private String salaryMonth;
    private Long saleId;
    @Column(unique = true) private Long reversalOf;
    private LocalDate date;
    private LocalDateTime recordedAt;
    private String recordedBy;
    private Long revision;
    private String lastEditRequestKey;
    private String lastEditFingerprint;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
