package com.controlm.config.application.port;

import com.controlm.config.infrastructure.persistence.SystemSettingEntity;
import java.util.Optional;
import java.util.UUID;

/** Outbound persistence port for {@code system_setting}. */
public interface SystemSettingRepository {

    SystemSettingEntity save(SystemSettingEntity setting);

    Optional<SystemSettingEntity> findById(UUID id);

    Optional<SystemSettingEntity> findBySettingKey(String settingKey);
}
