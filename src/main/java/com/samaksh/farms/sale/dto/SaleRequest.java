package com.samaksh.farms.sale.dto;

import com.samaksh.farms.enums.PaymentStatus;
import lombok.Data;

@Data
public class SaleRequest {

    private Long customerId;

    private Long productId;

    private Double quantity;

    private Double unitPrice;

    private PaymentStatus paymentStatus;

    private String remarks;
}