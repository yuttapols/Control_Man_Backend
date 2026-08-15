package com.controlm.iam.infrastructure.persistence;

import com.controlm.iam.application.port.RolePermissionRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Implements the {@link RolePermissionRepository} port by delegating to Spring Data. */
@Repository
class RolePermissionRepositoryImpl implements RolePermissionRepository {

    private final RolePermissionJpaRepository jpa;

    RolePermissionRepositoryImpl(RolePermissionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RolePermissionEntity save(RolePermissionEntity rolePermission) {
        return jpa.save(rolePermission);
    }

    @Override
    public Optional<RolePermissionEntity> findById(RolePermissionId id) {
        return jpa.findById(id);
    }
}
