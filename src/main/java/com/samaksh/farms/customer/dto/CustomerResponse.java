package com.samaksh.farms.customer.dto;

import com.samaksh.farms.enums.ExchangeType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerResponse {

    private Long id;

    private String customerName;

    private String contactPerson;

    private String phoneNumber;

    private String email;

    private String address;

    private String location;

    private String shopCategory;

    private List<String> products;

    private Double minimumBoxesPerDay;

    private Double dailyReturnedBoxes;

    private Double defaultBoxPrice;

    private Double shopkeeperSellingPrice;

    private ExchangeType exchangeType;

    private Boolean active;
}
