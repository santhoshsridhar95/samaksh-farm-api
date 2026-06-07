package com.samaksh.farms.customer.dto;

import lombok.Data;

@Data
public class CustomerRequest {

    private String customerName;

    private String contactPerson;

    private String phoneNumber;

    private String email;

    private String address;
}