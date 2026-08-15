package com.controlm.auth.application;

import com.controlm.iam.domain.AuthenticatedUser;
import java.util.List;

public record AuthResult(
        String accessToken,
        long expiresInSeconds,
        RefreshTokenResult refresh,
        AuthenticatedUser user,
        List<String> permissions) {
    public AuthResult {
        permissions = List.copyOf(permissions);
    }
}
