package com.samaksh.farms.sale.dto;

import com.samaksh.farms.enums.ExchangeType;
import com.samaksh.farms.enums.PaymentStatus;
import lombok.Data;

@Data
public class SaleRequest {

    private Long customerId;

    private Long productId;

    private String productName;

    private Double quantity;

    private Double unitPrice;

    private Double amountCollected;

    private Long collectorUserId;

    private String collectorName;

    private String collectorEmail;

    private Double shopkeeperSellingPrice;

    private ExchangeType exchangeType;

    private Double exchangeBoxes;

    private Double returnedBoxes;

    private PaymentStatus paymentStatus;

    private String remarks;
}
