package com.samaksh.farms.customer.entity;

import com.samaksh.farms.enums.ExchangeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;

    private String contactPerson;

    private String phoneNumber;

    private String email;

    private String address;

    private String location;

    private String shopCategory;

    @ElementCollection
    @CollectionTable(
            name = "customer_products",
            joinColumns = @JoinColumn(name = "customer_id")
    )
    @Column(name = "product_name")
    private List<String> products;

    private Double minimumBoxesPerDay;

    private Double dailyReturnedBoxes;

    private Double defaultBoxPrice;

    private Double shopkeeperSellingPrice;

    @Enumerated(EnumType.STRING)
    private ExchangeType exchangeType;

    private Boolean active;

    private LocalDateTime createdAt;
}
