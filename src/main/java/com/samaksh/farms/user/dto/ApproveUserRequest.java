package com.samaksh.farms.user.dto;

import com.samaksh.farms.enums.Role;
import lombok.Data;

import java.util.List;

@Data
public class ApproveUserRequest {

    private Role role;

    private List<Role> roles;

    private Boolean active;
}
