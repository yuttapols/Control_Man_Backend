package com.controlm.audit.application;

import com.controlm.audit.application.port.AuditLogRepository;
import com.controlm.audit.infrastructure.persistence.AuditLogEntity;
import com.controlm.shared.web.RequestIdHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditServiceImpl implements AuditService {
    private static final Set<String> SENSITIVE = Set.of(
            "password", "token", "authorization", "cookie", "secret", "apikey", "credential");
    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditServiceImpl(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditCommand command) {
        AuditLogEntity entry = new AuditLogEntity(command.actorType(), command.module(), command.action(),
                command.targetType(), command.targetId());
        entry.setActorUserId(command.actorUserId());
        entry.setCorrelationId(RequestIdHolder.currentRequestId());
        entry.setMetadata(toJson(redact(command.metadata())));
        repository.save(entry);
    }

    private Map<String, Object> redact(Map<String, ?> input) {
        Map<String, Object> safe = new LinkedHashMap<>();
        input.forEach((key, value) -> safe.put(key, isSensitive(key) ? "[REDACTED]" : safeValue(value)));
        return safe;
    }

    private boolean isSensitive(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        return SENSITIVE.stream().anyMatch(normalized::contains);
    }

    private Object safeValue(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) return value;
        if (value instanceof Enum<?> enumValue) return enumValue.name();
        return String.valueOf(value);
    }

    private String toJson(Map<String, Object> value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException ex) { return "{\"serializationError\":true}"; }
    }
}
