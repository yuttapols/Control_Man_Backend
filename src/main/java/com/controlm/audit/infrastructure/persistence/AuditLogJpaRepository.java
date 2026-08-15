package com.controlm.audit.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data adapter for {@code audit_log}; wrapped by {@link AuditLogRepositoryImpl}. */
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {
}
