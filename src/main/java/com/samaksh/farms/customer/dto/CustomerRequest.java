package com.samaksh.farms.customer.dto;

import com.samaksh.farms.enums.ExchangeType;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class CustomerRequest {

    private String customerName;

    private String contactPerson;

    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Phone number must be exactly 10 digits"
    )
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
