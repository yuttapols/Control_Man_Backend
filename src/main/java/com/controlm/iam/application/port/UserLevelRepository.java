package com.controlm.iam.application.port;

import com.controlm.iam.infrastructure.persistence.UserLevelEntity;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence port for {@code user_level}. Application code depends on this abstraction,
 * not on Spring Data, so the storage technology can change without touching use cases.
 */
public interface UserLevelRepository {

    UserLevelEntity save(UserLevelEntity userLevel);

    Optional<UserLevelEntity> findById(UUID id);
}
