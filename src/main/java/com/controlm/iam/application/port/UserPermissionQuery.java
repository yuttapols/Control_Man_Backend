package com.controlm.iam.application.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read-only authorization projection, kept separate from user aggregate persistence. */
public interface UserPermissionQuery {
    List<String> findActivePermissionCodes(UUID userId, Instant now);
}
