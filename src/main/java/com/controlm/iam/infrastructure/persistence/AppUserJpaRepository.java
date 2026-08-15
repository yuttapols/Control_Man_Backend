package com.controlm.iam.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data adapter for {@code app_user}; wrapped by {@link AppUserRepositoryImpl}. */
public interface AppUserJpaRepository extends JpaRepository<AppUserEntity, UUID> {

    Optional<AppUserEntity> findByUsernameIgnoreCase(String username);
}
