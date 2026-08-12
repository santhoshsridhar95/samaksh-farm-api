package com.samaksh.farms.server.service;

import com.samaksh.farms.server.dto.KeepAwakeSettingsRequest;
import com.samaksh.farms.server.dto.KeepAwakeSettingsResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class KeepAwakeService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(KeepAwakeService.class);

    private static final int MIN_INTERVAL_MINUTES = 1;

    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

    @Value("${app.keep-awake.enabled:false}")
    private boolean configuredEnabled;

    @Value("${app.keep-awake.interval-minutes:10}")
    private int configuredIntervalMinutes;

    @Value("${app.keep-awake.target-url:}")
    private String configuredTargetUrl;

    @Value("${app.keep-awake.render-external-url:}")
    private String renderExternalUrl;

    private volatile boolean enabled;

    private volatile int intervalMinutes;

    private volatile String targetUrl;

    private volatile LocalDateTime lastPingAt;

    private volatile Integer lastStatusCode;

    private volatile String lastMessage;

    @PostConstruct
    public void initialize() {
        enabled = configuredEnabled;
        intervalMinutes = normalizeInterval(configuredIntervalMinutes);
        targetUrl = resolveInitialTargetUrl();
        lastMessage = targetUrl.isBlank()
                ? "Keep-awake target URL is empty"
                : "Keep-awake initialized";
    }

    public KeepAwakeSettingsResponse settings() {
        return response();
    }

    public KeepAwakeSettingsResponse update(
            KeepAwakeSettingsRequest request
    ) {
        if (request.getEnabled() != null) {
            enabled = request.getEnabled();
        }

        if (request.getIntervalMinutes() != null) {
            intervalMinutes = normalizeInterval(request.getIntervalMinutes());
        }

        if (request.getTargetUrl() != null) {
            targetUrl = resolveTargetUrl(request.getTargetUrl());
        }

        lastMessage = "Settings updated for the running server";

        return response();
    }

    public KeepAwakeSettingsResponse pingNow() {
        pingTarget();
        return response();
    }

    @Scheduled(fixedDelay = 60000)
    public void scheduledPing() {
        if (!enabled || targetUrl.isBlank()) {
            return;
        }

        if (lastPingAt != null &&
                lastPingAt.plusMinutes(intervalMinutes).isAfter(LocalDateTime.now())) {
            return;
        }

        pingTarget();
    }

    private void pingTarget() {
        if (targetUrl.isBlank()) {
            lastMessage = "Keep-awake target URL is empty";
            return;
        }

        try {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(targetUrl))
                            .timeout(Duration.ofSeconds(20))
                            .GET()
                            .build();
            HttpResponse<Void> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.discarding()
                    );

            lastPingAt = LocalDateTime.now();
            lastStatusCode = response.statusCode();
            lastMessage = "Ping completed";
        } catch (Exception ex) {
            lastPingAt = LocalDateTime.now();
            lastStatusCode = null;
            lastMessage = ex.getMessage();
            LOGGER.warn(
                    "Keep-awake ping failed for {}",
                    targetUrl,
                    ex
            );
        }
    }

    private KeepAwakeSettingsResponse response() {
        return KeepAwakeSettingsResponse.builder()
                .enabled(enabled)
                .intervalMinutes(intervalMinutes)
                .targetUrl(targetUrl)
                .lastPingAt(lastPingAt)
                .lastStatusCode(lastStatusCode)
                .lastMessage(lastMessage)
                .build();
    }

    private int normalizeInterval(int value) {
        return Math.max(
                MIN_INTERVAL_MINUTES,
                value
        );
    }

    private String normalizeUrl(String value) {
        return value == null ? "" : value.trim();
    }

    private String resolveInitialTargetUrl() {
        return resolveTargetUrl(configuredTargetUrl);
    }

    private String resolveTargetUrl(
            String targetUrl
    ) {
        String explicitTarget =
                normalizeUrl(targetUrl);

        if (!explicitTarget.isBlank()) {
            return explicitTarget;
        }

        String renderUrl =
                normalizeUrl(renderExternalUrl);

        if (renderUrl.isBlank()) {
            return "";
        }

        return renderUrl.replaceAll("/+$", "") +
                "/api/health/ping";
    }
}
