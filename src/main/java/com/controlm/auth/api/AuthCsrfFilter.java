package com.controlm.auth.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthCsrfFilter extends OncePerRequestFilter {
    private final AuthWebProperties properties;
    private final AuthCookieService cookies;

    AuthCsrfFilter(AuthWebProperties properties, AuthCookieService cookies) {
        this.properties = properties;
        this.cookies = cookies;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !("POST".equals(request.getMethod())
                && (path.endsWith("/auth/refresh") || path.endsWith("/auth/logout")));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        String cookieToken = cookies.value(request.getCookies(), AuthCookieService.CSRF_COOKIE);
        String headerToken = request.getHeader(AuthCookieService.CSRF_HEADER);
        boolean originAllowed = origin != null && properties.allowedOrigins().contains(origin);
        boolean tokenMatches = cookieToken != null && headerToken != null && MessageDigest.isEqual(
                cookieToken.getBytes(StandardCharsets.UTF_8), headerToken.getBytes(StandardCharsets.UTF_8));
        if (!originAllowed || !tokenMatches) {
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }
        chain.doFilter(request, response);
    }
}
