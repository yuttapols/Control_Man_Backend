package com.controlm.iam.infrastructure.persistence;

import com.controlm.iam.application.port.UserLevelRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Implements the {@link UserLevelRepository} port by delegating to Spring Data. */
@Repository
class UserLevelRepositoryImpl implements UserLevelRepository {

    private final UserLevelJpaRepository jpa;

    UserLevelRepositoryImpl(UserLevelJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public UserLevelEntity save(UserLevelEntity userLevel) {
        return jpa.save(userLevel);
    }

    @Override
    public Optional<UserLevelEntity> findById(UUID id) {
        return jpa.findById(id);
    }
}
