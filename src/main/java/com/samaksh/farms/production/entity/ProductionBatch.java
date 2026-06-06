package com.samaksh.farms.production.entity;

import com.samaksh.farms.enums.BatchStatus;
import com.samaksh.farms.enums.MushroomType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "production_batch")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private MushroomType mushroomType;

    private String batchCode;

    private Double spawnUsed;

    private Double pelletsUsed;

    private Double bagsUsed;

    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    private BatchStatus status;
}