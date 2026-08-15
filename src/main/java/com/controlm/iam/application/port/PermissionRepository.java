package com.controlm.iam.application.port;

import com.controlm.iam.infrastructure.persistence.PermissionEntity;
import java.util.Optional;
import java.util.UUID;

/** Outbound persistence port for the {@code permission} catalogue. */
public interface PermissionRepository {

    PermissionEntity save(PermissionEntity permission);

    Optional<PermissionEntity> findById(UUID id);

    Optional<PermissionEntity> findByCode(String code);
}
