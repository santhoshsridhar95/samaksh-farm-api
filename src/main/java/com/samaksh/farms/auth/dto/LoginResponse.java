package com.samaksh.farms.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LoginResponse {

    private Long userId;

    private String token;

    private String role;

    private String name;

    private Boolean active;
}
