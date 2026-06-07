package com.samaksh.farms.order.dto;

import com.samaksh.farms.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CustomerOrderResponse {

    private Long id;

    private String orderCode;

    private String customerName;

    private String productName;

    private Double quantity;

    private Double expectedUnitPrice;

    private Double expectedAmount;

    private LocalDate expectedDeliveryDate;

    private OrderStatus status;

    private String remarks;
}