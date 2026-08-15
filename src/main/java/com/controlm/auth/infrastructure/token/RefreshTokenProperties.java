package com.controlm.auth.infrastructure.token;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.auth")
public record RefreshTokenProperties(Duration refreshTokenTtl, Duration refreshIdleTimeout) {
    public RefreshTokenProperties {
        requirePositive(refreshTokenTtl, "refresh-token TTL");
        requirePositive(refreshIdleTimeout, "refresh idle timeout");
        if (refreshIdleTimeout.compareTo(refreshTokenTtl) > 0) {
            throw new IllegalArgumentException("Refresh idle timeout cannot exceed token TTL");
        }
    }

    private static void requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
