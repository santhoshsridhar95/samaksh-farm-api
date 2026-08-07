package com.samaksh.farms.sale.dto;

import com.samaksh.farms.enums.ExchangeType;
import com.samaksh.farms.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SaleResponse {

    private Long id;

    private Long customerId;

    private String customerName;

    private String shopCategory;

    private String location;

    private Double minimumBoxesPerDay;

    private Long productId;

    private String productName;

    private Double quantity;

    private Double unitPrice;

    private Double totalAmount;

    private Double amountCollected;

    private Double pendingAmount;

    private Double shopkeeperSellingPrice;

    private ExchangeType exchangeType;

    private Double exchangeBoxes;

    private Double returnedBoxes;

    private PaymentStatus paymentStatus;

    private String remarks;

    private LocalDateTime saleDate;
}
