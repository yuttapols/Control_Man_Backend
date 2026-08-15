package com.controlm.iam.infrastructure.persistence;

import com.controlm.iam.application.port.RoleRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Implements the {@link RoleRepository} port by delegating to Spring Data. */
@Repository
class RoleRepositoryImpl implements RoleRepository {

    private final RoleJpaRepository jpa;

    RoleRepositoryImpl(RoleJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RoleEntity save(RoleEntity role) {
        return jpa.save(role);
    }

    @Override
    public Optional<RoleEntity> findById(UUID id) {
        return jpa.findById(id);
    }
}
