package com.samaksh.farms.server.service;

import com.samaksh.farms.server.dto.PublicContentSettingsRequest;
import com.samaksh.farms.server.dto.PublicContentSettingsResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PublicContentSettingsService {

    @Value("${app.public-content.dynamic-enabled:false}")
    private boolean configuredDynamicContentEnabled;

    private volatile boolean dynamicContentEnabled;

    @PostConstruct
    public void initialize() {
        dynamicContentEnabled = configuredDynamicContentEnabled;
    }

    public PublicContentSettingsResponse settings() {
        return response();
    }

    public PublicContentSettingsResponse update(
            PublicContentSettingsRequest request
    ) {
        if (request.getDynamicContentEnabled() != null) {
            dynamicContentEnabled = request.getDynamicContentEnabled();
        }

        return response();
    }

    private PublicContentSettingsResponse response() {
        return PublicContentSettingsResponse.builder()
                .dynamicContentEnabled(dynamicContentEnabled)
                .build();
    }
}
