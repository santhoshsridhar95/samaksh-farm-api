package com.samaksh.farms.server.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class KeepAwakeSettingsResponse {

    private boolean enabled;

    private int intervalMinutes;

    private String targetUrl;

    private LocalDateTime lastPingAt;

    private Integer lastStatusCode;

    private String lastMessage;
}
