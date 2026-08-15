package com.controlm.iam.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data adapter for {@code user_role}; wrapped by {@link UserRoleRepositoryImpl}. */
public interface UserRoleJpaRepository extends JpaRepository<UserRoleEntity, UserRoleId> {
}
