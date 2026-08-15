package com.controlm.iam.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data adapter for {@code role}; wrapped by {@link RoleRepositoryImpl}. */
public interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {
}
