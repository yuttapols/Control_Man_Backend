package com.controlm.auth.api;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.auth-web")
public record AuthWebProperties(String cookieDomain, boolean secureCookie, List<String> allowedOrigins) {
    public AuthWebProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
