package com.samaksh.farms.order.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomerOrderRequest {

    private Long customerId;

    private Long productId;

    private Double quantity;

    private Double expectedUnitPrice;

    private LocalDate expectedDeliveryDate;

    private String remarks;
}