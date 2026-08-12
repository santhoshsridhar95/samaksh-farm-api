package com.samaksh.farms.entitlement.dto;

import com.samaksh.farms.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EntitlementResponse {

    private Role role;

    private List<String> permissions;
}
