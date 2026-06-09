package com.samaksh.farms.user.dto;

import com.samaksh.farms.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeRoleRequest {

    @NotNull
    private Role role;
}
