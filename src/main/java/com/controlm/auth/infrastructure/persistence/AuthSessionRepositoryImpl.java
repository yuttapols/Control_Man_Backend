package com.controlm.auth.infrastructure.persistence;

import com.controlm.auth.application.port.AuthSessionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Implements the {@link AuthSessionRepository} port by delegating to Spring Data. */
@Repository
class AuthSessionRepositoryImpl implements AuthSessionRepository {

    private final AuthSessionJpaRepository jpa;

    AuthSessionRepositoryImpl(AuthSessionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AuthSessionEntity save(AuthSessionEntity session) {
        return jpa.save(session);
    }

    @Override
    public Optional<AuthSessionEntity> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<AuthSessionEntity> findByRefreshTokenHash(String refreshTokenHash) {
        return jpa.findByRefreshTokenHash(refreshTokenHash);
    }
}
