package com.controlm.iam.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

/** Spring Data adapter for {@code app_user}; wrapped by {@link AppUserRepositoryImpl}. */
public interface AppUserJpaRepository extends JpaRepository<AppUserEntity, UUID> {

    Optional<AppUserEntity> findByUsernameIgnoreCase(String username);

    @Query("""
            select distinct permission.code
            from UserRoleEntity assignment
            join RoleEntity role on role.id = assignment.id.roleId
            join RolePermissionEntity grant on grant.id.roleId = role.id
            join PermissionEntity permission on permission.id = grant.id.permissionId
            where assignment.id.userId = :userId
              and role.status = com.controlm.iam.domain.ActivationStatus.ACTIVE
              and assignment.validFrom <= :now
              and (assignment.validUntil is null or assignment.validUntil > :now)
            """)
    List<String> findActivePermissionCodes(@Param("userId") UUID userId, @Param("now") Instant now);
}
