package com.controlm.iam.infrastructure.persistence;

import com.controlm.iam.application.port.PermissionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Implements the {@link PermissionRepository} port by delegating to Spring Data. */
@Repository
class PermissionRepositoryImpl implements PermissionRepository {

    private final PermissionJpaRepository jpa;

    PermissionRepositoryImpl(PermissionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public PermissionEntity save(PermissionEntity permission) {
        return jpa.save(permission);
    }

    @Override
    public Optional<PermissionEntity> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<PermissionEntity> findByCode(String code) {
        return jpa.findByCode(code);
    }
}
