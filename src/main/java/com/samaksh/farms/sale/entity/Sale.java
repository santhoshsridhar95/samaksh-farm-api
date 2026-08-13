package com.samaksh.farms.sale.entity;

import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.enums.ExchangeType;
import com.samaksh.farms.enums.PaymentStatus;
import com.samaksh.farms.order.entity.CustomerOrder;
import com.samaksh.farms.products.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sales")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Customer customer;

    @ManyToOne
    private Product product;

    @ManyToOne
    private CustomerOrder order;

    private Double quantity;

    private Double unitPrice;

    private Double totalAmount;

    private Double amountCollected;

    private Double pendingAmount;

    private Double shopkeeperSellingPrice;

    @Enumerated(EnumType.STRING)
    private ExchangeType exchangeType;

    private Double exchangeBoxes;

    private Double returnedBoxes;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private String remarks;

    private Long createdByUserId;

    private String createdByName;

    private String createdByEmail;

    private Long collectorUserId;

    private String collectorName;

    private String collectorEmail;

    private Long updatedByUserId;

    private String updatedByName;

    private String updatedByEmail;

    private LocalDateTime updatedAt;

    private LocalDateTime saleDate;
}
