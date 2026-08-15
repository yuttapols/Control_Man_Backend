package com.controlm.auth.application.port;

import com.controlm.auth.infrastructure.persistence.AuthSessionEntity;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence port for {@code auth_session}. Sessions are looked up by the hash of the
 * presented refresh token, never by the token itself.
 */
public interface AuthSessionRepository {

    AuthSessionEntity save(AuthSessionEntity session);

    Optional<AuthSessionEntity> findById(UUID id);

    Optional<AuthSessionEntity> findByRefreshTokenHash(String refreshTokenHash);
}
