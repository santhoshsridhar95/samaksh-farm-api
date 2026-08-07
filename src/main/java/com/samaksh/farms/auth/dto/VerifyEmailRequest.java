package com.samaksh.farms.auth.dto;

import lombok.Data;

@Data
public class VerifyEmailRequest {

    private String email;

    private String otp;

    private String token;
}
