package com.controlm.auth.infrastructure.persistence;

import com.controlm.auth.application.port.AuthSessionRepository;
import java.util.List;
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

    @Override
    public Optional<AuthSessionEntity> findByRefreshTokenHashForUpdate(String refreshTokenHash) {
        return jpa.findByRefreshTokenHashForUpdate(refreshTokenHash);
    }

    @Override
    public List<AuthSessionEntity> findFamilyForUpdate(UUID tokenFamilyId) {
        return jpa.findFamilyForUpdate(tokenFamilyId);
    }
}
