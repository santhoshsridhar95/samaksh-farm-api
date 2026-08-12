package com.samaksh.farms.server.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KeepAwakeSettingsRequest {

    private Boolean enabled;

    private Integer intervalMinutes;

    private String targetUrl;
}
