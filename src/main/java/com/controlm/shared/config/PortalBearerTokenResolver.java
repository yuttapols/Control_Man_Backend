package com.controlm.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/**
 * Ignores any bearer token presented to the public auth endpoints.
 *
 * <p>{@code login}/{@code refresh}/{@code logout} are {@code permitAll} and must work off the
 * refresh cookie alone. Without this resolver a stale or expired access token — which the SPA's
 * HTTP interceptor may attach to every call — would be validated by the resource-server filter and
 * rejected with 401 <em>before</em> {@code permitAll} ever applies, breaking session restore on
 * page reload. Every other request (including {@code /auth/me}) keeps the default behaviour.
 */
class PortalBearerTokenResolver implements BearerTokenResolver {
    private static final Set<String> TOKEN_LESS_AUTH_PATHS = Set.of(
            "/api/v1/portal/auth/login",
            "/api/v1/portal/auth/refresh",
            "/api/v1/portal/auth/logout");

    private final BearerTokenResolver delegate = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        if ("POST".equals(request.getMethod()) && TOKEN_LESS_AUTH_PATHS.contains(request.getRequestURI())) {
            return null;
        }
        return delegate.resolve(request);
    }
}
