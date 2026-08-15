package com.controlm.auth.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data adapter for {@code auth_session}; wrapped by {@link AuthSessionRepositoryImpl}. */
public interface AuthSessionJpaRepository extends JpaRepository<AuthSessionEntity, UUID> {

    Optional<AuthSessionEntity> findByRefreshTokenHash(String refreshTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuthSessionEntity s where s.refreshTokenHash = :hash")
    Optional<AuthSessionEntity> findByRefreshTokenHashForUpdate(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuthSessionEntity s where s.tokenFamilyId = :familyId")
    List<AuthSessionEntity> findFamilyForUpdate(@Param("familyId") UUID familyId);
}
