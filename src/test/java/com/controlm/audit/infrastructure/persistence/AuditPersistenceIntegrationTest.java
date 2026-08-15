package com.controlm.audit.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlm.audit.application.AuditCommand;
import com.controlm.audit.application.AuditServiceImpl;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@Tag("db")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditPersistenceIntegrationTest {
    @Autowired private AuditLogJpaRepository repository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("audit event persist เป็น append-only JSONB พร้อมค่าที่ redact แล้ว")
    void auditEventPersistsAsRedactedJsonb() {
        AuditServiceImpl service = new AuditServiceImpl(new AuditLogRepositoryImpl(repository));
        String action = "TEST_AUDIT_" + System.nanoTime();

        service.record(AuditCommand.system("auth", action, "AUTHENTICATION", "anonymous",
                Map.of("outcome", "DENIED", "accessToken", "must-not-persist")));
        entityManager.flush();
        entityManager.clear();

        AuditLogEntity saved = repository.findAll().stream()
                .filter(event -> action.equals(event.getAction()))
                .findFirst()
                .orElseThrow();
        assertThat(saved.getOccurredAt()).isNotNull();
        assertThat(saved.getMetadata()).contains("DENIED", "[REDACTED]").doesNotContain("must-not-persist");
    }
}
