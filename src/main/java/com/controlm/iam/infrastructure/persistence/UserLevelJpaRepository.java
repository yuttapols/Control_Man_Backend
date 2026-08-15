package com.controlm.iam.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data adapter for {@code user_level}; wrapped by {@link UserLevelRepositoryImpl}. */
public interface UserLevelJpaRepository extends JpaRepository<UserLevelEntity, UUID> {
}
