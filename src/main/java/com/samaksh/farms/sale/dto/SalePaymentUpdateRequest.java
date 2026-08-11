package com.samaksh.farms.sale.dto;

import lombok.Data;

@Data
public class SalePaymentUpdateRequest {

    private Double amountCollected;

    private String remarks;
}
