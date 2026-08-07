package com.samaksh.farms.user.dto;

import com.samaksh.farms.enums.Role;
import lombok.Data;

@Data
public class ApproveUserRequest {

    private Role role;

    private Boolean active;
}
