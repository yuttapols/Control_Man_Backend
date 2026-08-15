package com.controlm.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlm.audit.application.port.AuditLogRepository;
import com.controlm.audit.infrastructure.persistence.AuditLogEntity;
import com.controlm.shared.web.RequestIdHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

class AuditServiceImplTest {
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("audit เติม actor/request ID และ redact credential fields ก่อน persist")
    void recordsRedactedMetadataAndCorrelationId() throws Exception {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AuditService service = new AuditServiceImpl(repository);
        UUID userId = UUID.randomUUID();
        MDC.put(RequestIdHolder.MDC_KEY, "request-123");

        service.record(AuditCommand.user(userId, "auth", "LOGIN_SUCCESS", "USER", userId.toString(),
                Map.of("outcome", "SUCCESS", "password", "do-not-store", "refreshToken", "secret")));

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repository).save(captor.capture());
        AuditLogEntity entry = captor.getValue();
        assertThat(entry.getActorUserId()).isEqualTo(userId);
        assertThat(entry.getCorrelationId()).isEqualTo("request-123");
        assertThat(entry.getMetadata()).contains("SUCCESS", "[REDACTED]")
                .doesNotContain("do-not-store", "secret");
    }

    @Test
    @DisplayName("audit metadata ยอมรับเฉพาะ scalar และแปลง object อื่นเป็นข้อความ")
    void nonScalarMetadataCannotBecomeArbitraryJsonGraph() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditService service = new AuditServiceImpl(repository);

        service.record(AuditCommand.system("auth", "LOGIN_FAILURE", "AUTHENTICATION", "anonymous",
                Map.of("detail", new StringBuilder("safe-summary"))));

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getMetadata()).isEqualTo("{\"detail\":\"safe-summary\"}");
    }
}
