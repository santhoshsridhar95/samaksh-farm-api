package com.samaksh.farms.auth.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ServerSessionState {

    private final boolean invalidateOnRestart;

    private final String serverSessionId =
            UUID.randomUUID().toString();

    private final Instant startedAt =
            Instant.now();

    public ServerSessionState(
            @Value("${app.security.jwt.invalidate-on-restart:true}")
            boolean invalidateOnRestart
    ) {

        this.invalidateOnRestart = invalidateOnRestart;
    }

    public boolean isInvalidateOnRestart() {
        return invalidateOnRestart;
    }

    public String getServerSessionId() {
        return serverSessionId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }
}
