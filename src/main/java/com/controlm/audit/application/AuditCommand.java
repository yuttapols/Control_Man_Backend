package com.controlm.audit.application;

import com.controlm.audit.domain.ActorType;
import java.util.Map;
import java.util.UUID;

public record AuditCommand(ActorType actorType, UUID actorUserId, String module, String action,
        String targetType, String targetId, Map<String, ?> metadata) {
    public AuditCommand { metadata = metadata == null ? Map.of() : Map.copyOf(metadata); }
    public static AuditCommand user(UUID userId, String module, String action, String type, String id,
            Map<String, ?> metadata) {
        return new AuditCommand(ActorType.USER, userId, module, action, type, id, metadata);
    }
    public static AuditCommand system(String module, String action, String type, String id,
            Map<String, ?> metadata) {
        return new AuditCommand(ActorType.SYSTEM, null, module, action, type, id, metadata);
    }
}
