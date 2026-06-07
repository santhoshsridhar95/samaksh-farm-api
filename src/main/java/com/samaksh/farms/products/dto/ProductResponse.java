package com.samaksh.farms.products.dto;

import com.samaksh.farms.enums.ProductUnitType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductResponse {

    private Long id;

    private String productCode;

    private String productName;

    private ProductUnitType unitType;

    private Double standardPrice;

    private Boolean active;
}