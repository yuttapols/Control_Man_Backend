package com.controlm.config.infrastructure.persistence;

import com.controlm.config.application.port.SystemSettingRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Implements the {@link SystemSettingRepository} port by delegating to Spring Data. */
@Repository
class SystemSettingRepositoryImpl implements SystemSettingRepository {

    private final SystemSettingJpaRepository jpa;

    SystemSettingRepositoryImpl(SystemSettingJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public SystemSettingEntity save(SystemSettingEntity setting) {
        return jpa.save(setting);
    }

    @Override
    public Optional<SystemSettingEntity> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<SystemSettingEntity> findBySettingKey(String settingKey) {
        return jpa.findBySettingKey(settingKey);
    }
}
