package com.samaksh.farms.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChangeRoleRequest {

    private String role;

    private List<String> roles;
}
