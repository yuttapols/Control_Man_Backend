package com.controlm.iam.infrastructure.persistence;

import com.controlm.iam.application.port.UserRoleRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Implements the {@link UserRoleRepository} port by delegating to Spring Data. */
@Repository
class UserRoleRepositoryImpl implements UserRoleRepository {

    private final UserRoleJpaRepository jpa;

    UserRoleRepositoryImpl(UserRoleJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public UserRoleEntity save(UserRoleEntity userRole) {
        return jpa.save(userRole);
    }

    @Override
    public Optional<UserRoleEntity> findById(UserRoleId id) {
        return jpa.findById(id);
    }
}
