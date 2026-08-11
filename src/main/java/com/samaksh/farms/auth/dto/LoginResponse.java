package com.samaksh.farms.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class LoginResponse {

    private Long userId;

    private String token;

    private String role;

    private List<String> roles;

    private String name;

    private Boolean active;
}
