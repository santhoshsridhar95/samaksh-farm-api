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

    /*
     * Labour Input
     */

    private Integer bagsPrepared;

    private Integer damagedCovers;

    private Double damagedSpawnKg;

    private Double damagedPelletsKg;

    /*
     * System Calculated
     */

    private Double spawnUsedKg;

    private Double pelletsUsedKg;

    private Integer coversUsed;

    /*
     * Cultivation Tracking
     */

    private Integer darkRoomBags;

    private Integer lightRoomBags;

    private Integer contaminatedBags;

    private Integer discardedBags;

    private String remarks;

    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    private BatchStatus status;
}