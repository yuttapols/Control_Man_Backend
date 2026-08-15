package com.controlm.auth.application;

import com.controlm.auth.infrastructure.jwt.JwtProperties;
import com.controlm.iam.application.AuthenticateCredentialsUseCase;
import com.controlm.iam.application.port.AppUserRepository;
import com.controlm.iam.application.port.UserPermissionQuery;
import com.controlm.iam.domain.AuthenticatedUser;
import com.controlm.iam.domain.UserStatus;
import com.controlm.shared.error.ApiException;
import com.controlm.shared.error.ErrorCode;
import com.controlm.audit.application.AuditCommand;
import com.controlm.audit.application.AuditService;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PortalAuthService {
    private final AuthenticateCredentialsUseCase credentials;
    private final RefreshTokenService refreshTokens;
    private final AccessTokenService accessTokens;
    private final AppUserRepository users;
    private final UserPermissionQuery permissions;
    private final JwtProperties jwt;
    private final AuditService audit;
    private final Clock clock;

    @Autowired
    public PortalAuthService(AuthenticateCredentialsUseCase credentials, RefreshTokenService refreshTokens,
            AccessTokenService accessTokens, AppUserRepository users, UserPermissionQuery permissions,
            JwtProperties jwt, AuditService audit) {
        this(credentials, refreshTokens, accessTokens, users, permissions, jwt, audit, Clock.systemUTC());
    }

    PortalAuthService(AuthenticateCredentialsUseCase credentials, RefreshTokenService refreshTokens,
            AccessTokenService accessTokens, AppUserRepository users, UserPermissionQuery permissions,
            JwtProperties jwt, AuditService audit, Clock clock) {
        this.credentials = credentials;
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.users = users;
        this.permissions = permissions;
        this.jwt = jwt;
        this.audit = audit;
        this.clock = clock;
    }

    public AuthResult login(String username, String password, String fingerprint) {
        try {
            AuthenticatedUser user = credentials.authenticate(username, password);
            RefreshTokenResult refresh = refreshTokens.create(user.id(), fingerprint);
            AuthResult result = result(user, refresh);
            audit.record(AuditCommand.user(user.id(), "auth", "LOGIN_SUCCESS", "USER", user.id().toString(),
                    Map.of("clientFingerprint", fingerprint, "outcome", "SUCCESS")));
            return result;
        } catch (ApiException ex) {
            audit.record(AuditCommand.system("auth", "LOGIN_FAILURE", "AUTHENTICATION", "anonymous",
                    Map.of("clientFingerprint", fingerprint, "outcome", "DENIED")));
            throw ex;
        }
    }

    public AuthResult refresh(String rawRefreshToken, String fingerprint) {
        try {
            RefreshTokenResult refresh = refreshTokens.rotate(rawRefreshToken, fingerprint);
            AuthenticatedUser user = currentUser(refresh.userId());
            AuthResult result = result(user, refresh);
            audit.record(AuditCommand.user(user.id(), "auth", "REFRESH_SUCCESS", "SESSION",
                    refresh.sessionId().toString(), Map.of("clientFingerprint", fingerprint, "outcome", "SUCCESS")));
            return result;
        } catch (ApiException ex) {
            audit.record(AuditCommand.system("auth", "REFRESH_FAILURE", "SESSION", "unknown",
                    Map.of("clientFingerprint", fingerprint, "outcome", "DENIED")));
            throw ex;
        }
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokens.revoke(rawRefreshToken, "LOGOUT");
        }
        audit.record(AuditCommand.system("auth", "LOGOUT", "SESSION", "current", Map.of("outcome", "SUCCESS")));
    }

    public AuthenticatedUser currentUser(UUID userId) {
        var user = users.findById(userId).filter(value -> value.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(PortalAuthService::unauthenticated);
        return new AuthenticatedUser(user.getId(), user.getUsername(), user.getDisplayName());
    }

    public UserProfile profile(UUID userId) {
        AuthenticatedUser user = currentUser(userId);
        return new UserProfile(user, permissions.findActivePermissionCodes(userId, clock.instant()));
    }

    private AuthResult result(AuthenticatedUser user, RefreshTokenResult refresh) {
        String access = accessTokens.issue(user, refresh.sessionId(), 0);
        List<String> active = permissions.findActivePermissionCodes(user.id(), clock.instant());
        return new AuthResult(access, jwt.accessTokenTtl().toSeconds(), refresh, user, active);
    }

    private static ApiException unauthenticated() {
        return new ApiException(ErrorCode.UNAUTHENTICATED, "Authentication required");
    }
}
