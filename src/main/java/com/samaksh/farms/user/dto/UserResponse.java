package com.samaksh.farms.user.dto;

import com.samaksh.farms.enums.ApprovalStatus;
import com.samaksh.farms.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private Long id;

    private String name;

    private String email;

    private String phoneNumber;

    private Role role;

    private Boolean active;

    private ApprovalStatus approvalStatus;
}
