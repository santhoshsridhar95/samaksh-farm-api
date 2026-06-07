package com.samaksh.farms.products.entity;

import com.samaksh.farms.enums.ProductUnitType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productCode;

    private String productName;

    @Enumerated(EnumType.STRING)
    private ProductUnitType unitType;

    private Double standardPrice;

    private Boolean active;

    private LocalDateTime createdAt;
}