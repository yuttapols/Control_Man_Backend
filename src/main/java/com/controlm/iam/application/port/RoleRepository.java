package com.controlm.iam.application.port;

import com.controlm.iam.infrastructure.persistence.RoleEntity;
import java.util.Optional;
import java.util.UUID;

/** Outbound persistence port for {@code role}. */
public interface RoleRepository {

    RoleEntity save(RoleEntity role);

    Optional<RoleEntity> findById(UUID id);
}
