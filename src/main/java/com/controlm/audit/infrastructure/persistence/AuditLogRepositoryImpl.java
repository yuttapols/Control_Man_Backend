package com.controlm.audit.infrastructure.persistence;

import com.controlm.audit.application.port.AuditLogRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Implements the {@link AuditLogRepository} port by delegating to Spring Data. */
@Repository
class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogJpaRepository jpa;

    AuditLogRepositoryImpl(AuditLogJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AuditLogEntity save(AuditLogEntity entry) {
        return jpa.save(entry);
    }

    @Override
    public Optional<AuditLogEntity> findById(UUID id) {
        return jpa.findById(id);
    }
}
