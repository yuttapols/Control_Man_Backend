package com.controlm.iam.application.port;

import com.controlm.iam.infrastructure.persistence.AppUserEntity;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence port for {@code app_user}. Lookups by username are case-insensitive to
 * match the {@code uq_app_user_username_ci} unique index enforced in Flyway V1.
 */
public interface AppUserRepository {

    AppUserEntity save(AppUserEntity user);

    Optional<AppUserEntity> findById(UUID id);

    Optional<AppUserEntity> findByUsernameIgnoreCase(String username);
}
