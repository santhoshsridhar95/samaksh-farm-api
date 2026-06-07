package com.samaksh.farms.customer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    private Boolean active;

    private LocalDateTime createdAt;
}