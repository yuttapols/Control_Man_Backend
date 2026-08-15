package com.controlm.iam.infrastructure.persistence;

import com.controlm.iam.application.port.AppUserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Implements the {@link AppUserRepository} port by delegating to Spring Data. */
@Repository
class AppUserRepositoryImpl implements AppUserRepository {

    private final AppUserJpaRepository jpa;

    AppUserRepositoryImpl(AppUserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AppUserEntity save(AppUserEntity user) {
        return jpa.save(user);
    }

    @Override
    public Optional<AppUserEntity> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<AppUserEntity> findByUsernameIgnoreCase(String username) {
        return jpa.findByUsernameIgnoreCase(username);
    }
}
