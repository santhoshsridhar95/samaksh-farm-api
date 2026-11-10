package com.samaksh.farms.productionmovement.entity;

import com.samaksh.farms.production.entity.ProductionBatch;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "production_movement")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionMovement {
    private Long performedByEmployeeId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private ProductionBatch batch;

    private Integer movedToLightRoom;

    private Integer contaminatedBags;

    private Integer discardedBags;

    private String remarks;

    private LocalDateTime movementDate;
}