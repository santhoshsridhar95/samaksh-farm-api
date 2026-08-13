package com.samaksh.farms.cashhandover.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cash_handovers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long collectorUserId;

    private String collectorName;

    private String collectorEmail;

    private String ownerName;

    private Double amount;

    private String remarks;

    private Long recordedByUserId;

    private String recordedByName;

    private String recordedByEmail;

    private LocalDateTime handedOverAt;

    private LocalDateTime updatedAt;
}
