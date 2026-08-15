package com.controlm.auth.infrastructure.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.jwt")
public record JwtProperties(
        String issuer,
        String audience,
        Duration accessTokenTtl,
        String privateKey,
        String publicKey) {

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) throw new IllegalArgumentException("JWT issuer is required");
        if (audience == null || audience.isBlank()) throw new IllegalArgumentException("JWT audience is required");
        if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("JWT access-token TTL must be positive");
        }
    }
}
