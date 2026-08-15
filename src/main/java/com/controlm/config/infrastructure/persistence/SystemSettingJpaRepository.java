package com.controlm.config.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data adapter for {@code system_setting}; wrapped by {@link SystemSettingRepositoryImpl}. */
public interface SystemSettingJpaRepository extends JpaRepository<SystemSettingEntity, UUID> {

    Optional<SystemSettingEntity> findBySettingKey(String settingKey);
}
