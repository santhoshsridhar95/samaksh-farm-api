package com.samaksh.farms.harvest.entity;

import com.samaksh.farms.production.entity.ProductionBatch;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "harvest")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Harvest {
    private Long performedByEmployeeId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate harvestDate;

    private Double quantity;
    private Integer numberOfBoxes;

    private String remarks;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    private ProductionBatch batch;

    private Long createdBy;

    private LocalDateTime createdAt;
}