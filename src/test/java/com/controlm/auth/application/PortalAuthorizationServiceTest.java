package com.controlm.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.controlm.auth.application.port.AuthSessionRepository;
import com.controlm.auth.infrastructure.persistence.AuthSessionEntity;
import com.controlm.iam.application.port.AppUserRepository;
import com.controlm.iam.application.port.UserPermissionQuery;
import com.controlm.iam.domain.UserStatus;
import com.controlm.iam.infrastructure.persistence.AppUserEntity;
import com.controlm.shared.error.ApiException;
import com.controlm.shared.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

class PortalAuthorizationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-15T12:00:00Z");
    private AppUserRepository users;
    private AuthSessionRepository sessions;
    private PortalAuthorizationService service;
    private UserPermissionQuery permissions;
    private UUID userId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        sessions = mock(AuthSessionRepository.class);
        permissions = mock(UserPermissionQuery.class);
        service = new PortalAuthorizationService(users, sessions, permissions, Clock.fixed(NOW, ZoneOffset.UTC));
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    @Test
    @DisplayName("active user และ active session ได้ authorities จาก permissions ปัจจุบัน")
    void activeIdentityGetsCurrentPermissions() {
        stubActiveIdentity();
        when(permissions.findActivePermissionCodes(userId, NOW))
                .thenReturn(List.of("holiday.revision.create", "audit.log.read"));

        PortalAuthorization result = service.authorize(jwt(userId, sessionId));

        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.permissions()).containsExactlyInAnyOrder("holiday.revision.create", "audit.log.read");
    }

    @Test
    @DisplayName("active user ที่ไม่มี permission ยืนยันตัวตนได้แต่มี authorities ว่างเพื่อ deny by default")
    void noPermissionProducesEmptyAuthorities() {
        stubActiveIdentity();
        when(permissions.findActivePermissionCodes(userId, NOW)).thenReturn(List.of());

        assertThat(service.authorize(jwt(userId, sessionId)).permissions()).isEmpty();
    }

    @Test
    @DisplayName("disabled user ถูกปฏิเสธแม้ JWT ถูกต้อง")
    void disabledUserIsRejected() {
        AppUserEntity user = user(UserStatus.DISABLED);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        assertUnauthenticated(jwt(userId, sessionId));
    }

    @Test
    @DisplayName("revoked session ถูกปฏิเสธแม้ JWT ยังไม่หมดอายุ")
    void revokedSessionIsRejected() {
        when(users.findById(userId)).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        AuthSessionEntity session = session();
        session.revoke(NOW.minusSeconds(1), "LOGOUT");
        when(sessions.findById(sessionId)).thenReturn(Optional.of(session));

        assertUnauthenticated(jwt(userId, sessionId));
    }

    @Test
    @DisplayName("session ที่เป็นของ user อื่นถูกปฏิเสธ")
    void sessionUserMismatchIsRejected() {
        when(users.findById(userId)).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        AuthSessionEntity session = new AuthSessionEntity(
                UUID.randomUUID(), "sha256:x", UUID.randomUUID(), NOW.plusSeconds(3600));
        when(sessions.findById(sessionId)).thenReturn(Optional.of(session));

        assertUnauthenticated(jwt(userId, sessionId));
    }

    private void stubActiveIdentity() {
        when(users.findById(userId)).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        when(sessions.findById(sessionId)).thenReturn(Optional.of(session()));
    }

    private AppUserEntity user(UserStatus status) {
        AppUserEntity user = new AppUserEntity("alice", "alice@example.com", "Alice", "hash");
        ReflectionTestUtils.setField(user, "id", userId);
        user.setStatus(status);
        return user;
    }

    private AuthSessionEntity session() {
        return new AuthSessionEntity(userId, "sha256:x", UUID.randomUUID(), NOW.plusSeconds(3600));
    }

    private static Jwt jwt(UUID userId, UUID sessionId) {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject(userId.toString())
                .claim("sid", sessionId.toString()).issuedAt(NOW).expiresAt(NOW.plusSeconds(900)).build();
    }

    private void assertUnauthenticated(Jwt jwt) {
        assertThatThrownBy(() -> service.authorize(jwt))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).errorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
    }
}
