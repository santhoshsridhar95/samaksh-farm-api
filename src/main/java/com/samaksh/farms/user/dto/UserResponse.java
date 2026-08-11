package com.samaksh.farms.user.dto;

import com.samaksh.farms.enums.ApprovalStatus;
import com.samaksh.farms.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserResponse {

    private Long id;

    private String name;

    private String email;

    private String phoneNumber;

    private Role role;

    private List<Role> roles;

    private Boolean active;

    private ApprovalStatus approvalStatus;

    private Boolean emailVerified;

    private List<String> extraPermissions;
}
