package com.samaksh.farms.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChangePermissionsRequest {

    private List<String> extraPermissions;
}
