package com.controlm.auth.application;

import java.time.Instant;
import java.util.UUID;

/** Raw refresh token is returned only at issue/rotation time and is never persisted. */
public record RefreshTokenResult(String token, UUID sessionId, UUID userId, Instant expiresAt) {}
