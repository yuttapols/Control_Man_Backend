package com.controlm.iam.application.port;

import com.controlm.iam.infrastructure.persistence.RolePermissionEntity;
import com.controlm.iam.infrastructure.persistence.RolePermissionId;
import java.util.Optional;

/** Outbound persistence port for {@code role_permission} grants. */
public interface RolePermissionRepository {

    RolePermissionEntity save(RolePermissionEntity rolePermission);

    Optional<RolePermissionEntity> findById(RolePermissionId id);
}
