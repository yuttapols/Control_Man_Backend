package com.controlm.auth.api;

import com.controlm.auth.application.AuthResult;
import com.controlm.auth.application.PortalAuthService;
import com.controlm.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portal/auth")
public class PortalAuthController {
    private final PortalAuthService auth;
    private final AuthCookieService cookies;

    public PortalAuthController(PortalAuthService auth, AuthCookieService cookies) {
        this.auth = auth;
        this.cookies = cookies;
    }

    @PostMapping("/login")
    ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http,
            HttpServletResponse response) {
        AuthResult result = auth.login(request.username(), request.password(), fingerprint(http));
        String csrf = cookies.setSession(response, result.refresh());
        return ApiResponse.of(AuthResponse.from(result, csrf));
    }

    @PostMapping("/refresh")
    ApiResponse<AuthResponse> refresh(HttpServletRequest http, HttpServletResponse response) {
        String raw = cookies.value(http.getCookies(), AuthCookieService.REFRESH_COOKIE);
        AuthResult result = auth.refresh(raw, fingerprint(http));
        String csrf = cookies.setSession(response, result.refresh());
        return ApiResponse.of(AuthResponse.from(result, csrf));
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(HttpServletRequest http, HttpServletResponse response) {
        auth.logout(cookies.value(http.getCookies(), AuthCookieService.REFRESH_COOKIE));
        cookies.clear(response);
        return ApiResponse.of(null);
    }

    @GetMapping("/me")
    ApiResponse<UserResponse> me(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        var profile = auth.profile(UUID.fromString(jwt.getSubject()));
        return ApiResponse.of(UserResponse.from(profile.user(), profile.permissions()));
    }

    private static String fingerprint(HttpServletRequest request) {
        String material = request.getHeader(HttpHeaders.USER_AGENT) + "|" + request.getHeader("Accept-Language");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
