package com.samaksh.farms.server.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicContentSettingsResponse {

    private boolean dynamicContentEnabled;
}
