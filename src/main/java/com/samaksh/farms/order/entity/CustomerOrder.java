package com.samaksh.farms.order.entity;

import com.samaksh.farms.customer.entity.Customer;
import com.samaksh.farms.enums.OrderStatus;
import com.samaksh.farms.products.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderCode;

    @ManyToOne
    private Customer customer;

    @ManyToOne
    private Product product;

    private Double quantity;

    private Double expectedUnitPrice;

    private Double expectedAmount;

    private LocalDate expectedDeliveryDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String remarks;

    private LocalDateTime createdAt;
}