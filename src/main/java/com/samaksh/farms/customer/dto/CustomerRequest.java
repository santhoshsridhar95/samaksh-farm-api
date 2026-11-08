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
            regexp = "^$|^[0-9]{10}$",
            message = "Phone number must be blank or exactly 10 digits"
    )
    private String phoneNumber;

    private String email;

    private String address;

    private String location;

    @jakarta.validation.constraints.Size(max = 2048)
    private String mapLocation;

    private String shopStage;

    private String shopCategory;

    private List<String> products;

    private Double minimumBoxesPerDay;

    private Double dailyReturnedBoxes;

    private Double defaultBoxPrice;

    private Double shopkeeperSellingPrice;

    private ExchangeType exchangeType;

    private Boolean active;
}
