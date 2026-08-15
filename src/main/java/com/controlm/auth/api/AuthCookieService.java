package com.controlm.auth.api;

import com.controlm.auth.application.RefreshTokenResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
class AuthCookieService {
    static final String REFRESH_COOKIE = "control_m_refresh";
    static final String CSRF_COOKIE = "control_m_csrf";
    static final String CSRF_HEADER = "X-CSRF-Token";
    static final String COOKIE_PATH = "/api/v1/portal/auth";
    private final AuthWebProperties properties;
    private final SecureRandom random = new SecureRandom();

    AuthCookieService(AuthWebProperties properties) {
        this.properties = properties;
    }

    String setSession(HttpServletResponse response, RefreshTokenResult refresh) {
        String csrf = randomToken();
        add(response, REFRESH_COOKIE, refresh.token(), true, Duration.between(java.time.Instant.now(), refresh.expiresAt()));
        add(response, CSRF_COOKIE, csrf, false, Duration.between(java.time.Instant.now(), refresh.expiresAt()));
        return csrf;
    }

    void clear(HttpServletResponse response) {
        add(response, REFRESH_COOKIE, "", true, Duration.ZERO);
        add(response, CSRF_COOKIE, "", false, Duration.ZERO);
    }

    String value(Cookie[] cookies, String name) {
        if (cookies == null) return null;
        for (Cookie cookie : cookies) if (name.equals(cookie.getName())) return cookie.getValue();
        return null;
    }

    private void add(HttpServletResponse response, String name, String value, boolean httpOnly, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(httpOnly).secure(properties.secureCookie())
                .sameSite(properties.secureCookie() ? "None" : "Lax")
                .path(COOKIE_PATH).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge);
        if (properties.cookieDomain() != null && !properties.cookieDomain().isBlank()) {
            builder.domain(properties.cookieDomain());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
