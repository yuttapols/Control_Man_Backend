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
    // Refresh cookie is scoped to the auth endpoints — the browser sends it by request path,
    // so a narrow path is fine and better for security.
    static final String REFRESH_COOKIE_PATH = "/api/v1/portal/auth";
    // CSRF cookie must be readable by JavaScript on every SPA route (double-submit), so it needs
    // path "/". document.cookie only exposes cookies whose Path is a prefix of the page path.
    static final String CSRF_COOKIE_PATH = "/";
    private final AuthWebProperties properties;
    private final SecureRandom random = new SecureRandom();

    AuthCookieService(AuthWebProperties properties) {
        this.properties = properties;
    }

    String setSession(HttpServletResponse response, RefreshTokenResult refresh) {
        String csrf = randomToken();
        Duration ttl = Duration.between(java.time.Instant.now(), refresh.expiresAt());
        add(response, REFRESH_COOKIE, refresh.token(), true, REFRESH_COOKIE_PATH, ttl);
        add(response, CSRF_COOKIE, csrf, false, CSRF_COOKIE_PATH, ttl);
        return csrf;
    }

    void clear(HttpServletResponse response) {
        // Path must match the set path per cookie, or the browser will not delete the cookie
        // (deletion matches on name + domain + path).
        add(response, REFRESH_COOKIE, "", true, REFRESH_COOKIE_PATH, Duration.ZERO);
        add(response, CSRF_COOKIE, "", false, CSRF_COOKIE_PATH, Duration.ZERO);
    }

    String value(Cookie[] cookies, String name) {
        if (cookies == null) return null;
        for (Cookie cookie : cookies) if (name.equals(cookie.getName())) return cookie.getValue();
        return null;
    }

    private void add(HttpServletResponse response, String name, String value, boolean httpOnly, String path, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(httpOnly).secure(properties.secureCookie())
                .sameSite(properties.secureCookie() ? "None" : "Lax")
                .path(path).maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge);
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
