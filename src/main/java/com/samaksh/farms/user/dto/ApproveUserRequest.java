package com.samaksh.farms.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class ApproveUserRequest {

    private String role;

    private List<String> roles;

    private Boolean active;
}
