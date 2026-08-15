package com.controlm.iam.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data adapter for {@code role_permission}; wrapped by {@link RolePermissionRepositoryImpl}. */
public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionEntity, RolePermissionId> {
}
