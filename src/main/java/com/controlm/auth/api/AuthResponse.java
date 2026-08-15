package com.controlm.auth.api;

import com.controlm.auth.application.AuthResult;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, String csrfToken, UserResponse user) {
    static AuthResponse from(AuthResult result, String csrfToken) {
        return new AuthResponse(result.accessToken(), "Bearer", result.expiresInSeconds(), csrfToken,
                UserResponse.from(result.user(), result.permissions()));
    }
}
