package com.samaksh.farms.sale.dto;

import com.samaksh.farms.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SaleResponse {

    private Long id;

    private String customerName;

    private String productName;

    private Double quantity;

    private Double unitPrice;

    private Double totalAmount;

    private PaymentStatus paymentStatus;

    private String remarks;
}