package com.samaksh.farms.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthConfigResponse {

    private boolean emailVerificationEnabled;
}
