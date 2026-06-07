package com.samaksh.farms.products.dto;

import com.samaksh.farms.enums.ProductUnitType;
import lombok.Data;

@Data
public class ProductRequest {

    private String productName;

    private ProductUnitType unitType;

    private Double standardPrice;
}