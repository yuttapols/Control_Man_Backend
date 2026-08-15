package com.controlm.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.controlm.audit.application.AuditService;
import com.controlm.auth.infrastructure.jwt.JwtProperties;
import com.controlm.iam.application.AuthenticateCredentialsUseCase;
import com.controlm.iam.application.port.AppUserRepository;
import com.controlm.iam.application.port.UserPermissionQuery;
import com.controlm.iam.domain.AuthenticatedUser;
import com.controlm.iam.domain.UserStatus;
import com.controlm.iam.infrastructure.persistence.AppUserEntity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PortalAuthServiceTest {

    private final Instant now = Instant.parse("2026-08-16T00:00:00Z");
    private AuthenticateCredentialsUseCase credentials;
    private RefreshTokenService refreshTokens;
    private AccessTokenService accessTokens;
    private AppUserRepository users;
    private UserPermissionQuery permissions;
    private AuditService audit;
    private PortalAuthService service;

    @BeforeEach
    void setUp() {
        credentials = mock(AuthenticateCredentialsUseCase.class);
        refreshTokens = mock(RefreshTokenService.class);
        accessTokens = mock(AccessTokenService.class);
        users = mock(AppUserRepository.class);
        permissions = mock(UserPermissionQuery.class);
        audit = mock(AuditService.class);
        JwtProperties jwt = new JwtProperties(
                "https://control-m.local", "control-m-portal", Duration.ofMinutes(15), null, null);
        service = new PortalAuthService(credentials, refreshTokens, accessTokens, users, permissions, jwt, audit,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("login แนบ permission ที่ active ของผู้ใช้ไปกับผลลัพธ์")
    void loginIncludesActivePermissions() {
        UUID userId = UUID.randomUUID();
        var user = new AuthenticatedUser(userId, "alice", "Alice");
        when(credentials.authenticate("alice", "secret")).thenReturn(user);
        when(refreshTokens.create(eq(userId), anyString()))
                .thenReturn(new RefreshTokenResult("raw", UUID.randomUUID(), userId, now.plusSeconds(3600)));
        when(accessTokens.issue(eq(user), any(), anyLong())).thenReturn("signed-access");
        when(permissions.findActivePermissionCodes(userId, now))
                .thenReturn(List.of("holiday.revision.submit", "holiday.revision.view"));

        AuthResult result = service.login("alice", "secret", "fp");

        assertThat(result.accessToken()).isEqualTo("signed-access");
        assertThat(result.permissions())
                .containsExactly("holiday.revision.submit", "holiday.revision.view");
    }

    @Test
    @DisplayName("profile คืน user ที่ ACTIVE พร้อม permission ณ เวลาปัจจุบัน")
    void profileReturnsUserWithPermissions() {
        UUID userId = UUID.randomUUID();
        AppUserEntity entity = mock(AppUserEntity.class);
        when(entity.getId()).thenReturn(userId);
        when(entity.getUsername()).thenReturn("alice");
        when(entity.getDisplayName()).thenReturn("Alice");
        when(entity.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(users.findById(userId)).thenReturn(Optional.of(entity));
        when(permissions.findActivePermissionCodes(userId, now)).thenReturn(List.of("holiday.revision.submit"));

        UserProfile profile = service.profile(userId);

        assertThat(profile.user().username()).isEqualTo("alice");
        assertThat(profile.permissions()).containsExactly("holiday.revision.submit");
    }

    @Test
    @DisplayName("profile ปฏิเสธผู้ใช้ที่ไม่ ACTIVE ด้วย 401 และไม่ไปแตะ permission")
    void profileRejectsInactiveUser() {
        UUID userId = UUID.randomUUID();
        AppUserEntity entity = mock(AppUserEntity.class);
        when(entity.getStatus()).thenReturn(UserStatus.SUSPENDED);
        when(users.findById(userId)).thenReturn(Optional.of(entity));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.profile(userId))
                .isInstanceOf(com.controlm.shared.error.ApiException.class);
    }
}
