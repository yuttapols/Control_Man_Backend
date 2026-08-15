package com.controlm.iam.application.port;

import com.controlm.iam.infrastructure.persistence.UserRoleEntity;
import com.controlm.iam.infrastructure.persistence.UserRoleId;
import java.util.Optional;

/** Outbound persistence port for {@code user_role} assignments. */
public interface UserRoleRepository {

    UserRoleEntity save(UserRoleEntity userRole);

    Optional<UserRoleEntity> findById(UserRoleId id);
}
