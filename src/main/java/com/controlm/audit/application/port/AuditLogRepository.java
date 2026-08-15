package com.controlm.audit.application.port;

import com.controlm.audit.infrastructure.persistence.AuditLogEntity;
import java.util.Optional;
import java.util.UUID;

/** Outbound persistence port for the append-only {@code audit_log}. */
public interface AuditLogRepository {

    AuditLogEntity save(AuditLogEntity entry);

    Optional<AuditLogEntity> findById(UUID id);
}
