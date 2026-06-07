package com.samaksh.farms.user.dto;

import com.samaksh.farms.enums.Role;
import lombok.Data;

@Data
public class ChangeRoleRequest {

    private Role role;
}