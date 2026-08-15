package com.controlm.auth.application;

import com.controlm.auth.application.port.AuthSessionRepository;
import com.controlm.iam.application.port.AppUserRepository;
import com.controlm.iam.application.port.UserPermissionQuery;
import com.controlm.iam.domain.UserStatus;
import com.controlm.shared.error.ApiException;
import com.controlm.shared.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PortalAuthorizationService {
    private final AppUserRepository users;
    private final AuthSessionRepository sessions;
    private final UserPermissionQuery permissions;
    private final Clock clock;

    @Autowired
    public PortalAuthorizationService(AppUserRepository users, AuthSessionRepository sessions,
            UserPermissionQuery permissions) {
        this(users, sessions, permissions, Clock.systemUTC());
    }

    PortalAuthorizationService(AppUserRepository users, AuthSessionRepository sessions,
            UserPermissionQuery permissions, Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.permissions = permissions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PortalAuthorization authorize(Jwt jwt) {
        Instant now = clock.instant();
        UUID userId = uuid(jwt.getSubject());
        UUID sessionId = uuid(jwt.getClaimAsString("sid"));
        var user = users.findById(userId)
                .filter(value -> value.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(PortalAuthorizationService::unauthenticated);
        var session = sessions.findById(sessionId)
                .filter(value -> value.getUserId().equals(userId))
                .filter(value -> value.getRevokedAt() == null)
                .filter(value -> value.getExpiresAt().isAfter(now))
                .orElseThrow(PortalAuthorizationService::unauthenticated);
        Set<String> currentPermissions = Set.copyOf(permissions.findActivePermissionCodes(userId, now));
        return new PortalAuthorization(user.getUsername(), currentPermissions);
    }

    private static UUID uuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException ex) {
            throw unauthenticated();
        }
    }

    private static ApiException unauthenticated() {
        return new ApiException(ErrorCode.UNAUTHENTICATED, "Authentication required");
    }
}
