package com.samaksh.farms.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    @JsonAlias({
            "login",
            "username",
            "phoneNumber",
            "mobileNumber"
    })
    private String email;

    @NotBlank
    private String password;

}
