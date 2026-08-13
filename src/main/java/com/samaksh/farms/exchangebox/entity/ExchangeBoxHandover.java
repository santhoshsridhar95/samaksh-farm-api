package com.samaksh.farms.exchangebox.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_box_handovers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeBoxHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long collectorUserId;

    private String collectorName;

    private String collectorEmail;

    private String ownerName;

    private Double boxes;

    private String remarks;

    private Long recordedByUserId;

    private String recordedByName;

    private String recordedByEmail;

    private LocalDateTime receivedAt;
}
