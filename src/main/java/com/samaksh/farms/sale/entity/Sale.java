package com.samaksh.farms.sale.entity;

import com.samaksh.farms.customer.entity.Customer;
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

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private String remarks;

    private LocalDateTime saleDate;
}