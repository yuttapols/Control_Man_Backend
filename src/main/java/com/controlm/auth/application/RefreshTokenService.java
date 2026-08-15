package com.controlm.auth.application;

import java.util.UUID;

public interface RefreshTokenService {
    RefreshTokenResult create(UUID userId, String clientFingerprint);
    RefreshTokenResult rotate(String presentedToken, String clientFingerprint);
    void revoke(String presentedToken, String reason);
}
