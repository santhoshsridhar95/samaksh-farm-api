package com.samaksh.farms.customer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerResponse {

    private Long id;

    private String customerName;

    private String contactPerson;

    private String phoneNumber;

    private String email;

    private String address;

    private Boolean active;
}