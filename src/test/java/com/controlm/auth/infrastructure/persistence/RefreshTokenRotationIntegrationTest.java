package com.controlm.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.controlm.auth.application.RefreshTokenResult;
import com.controlm.auth.application.RefreshTokenService;
import com.controlm.auth.application.RefreshTokenServiceImpl;
import com.controlm.auth.infrastructure.token.RefreshTokenCodec;
import com.controlm.auth.infrastructure.token.RefreshTokenProperties;
import com.controlm.iam.infrastructure.persistence.AppUserEntity;
import com.controlm.iam.infrastructure.persistence.AppUserJpaRepository;
import com.controlm.shared.error.ApiException;
import com.controlm.shared.error.ErrorCode;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@Tag("db")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRotationIntegrationTest {
    @Autowired private AuthSessionJpaRepository authJpa;
    @Autowired private AppUserJpaRepository userJpa;
    @Autowired private EntityManager entityManager;

    private RefreshTokenService service;
    private RefreshTokenCodec codec;
    private AppUserEntity user;

    @BeforeEach
    void setUp() {
        codec = new RefreshTokenCodec();
        service = new RefreshTokenServiceImpl(
                new AuthSessionRepositoryImpl(authJpa),
                codec,
                new RefreshTokenProperties(Duration.ofDays(7), Duration.ofHours(24)));
        user = userJpa.save(new AppUserEntity(
                "refresh-" + System.nanoTime(),
                "refresh-" + System.nanoTime() + "@example.com",
                "Refresh Test",
                "{noop}unused"));
        entityManager.flush();
    }

    @Test
    @DisplayName("สร้าง session แล้ว rotate โดยเก็บเฉพาะ hash และคง absolute expiry เดิม")
    void createsAndRotatesWithoutPersistingRawToken() {
        RefreshTokenResult first = service.create(user.getId(), "browser-a");
        entityManager.flush();
        RefreshTokenResult second = service.rotate(first.token(), "browser-a");
        entityManager.flush();

        AuthSessionEntity old = authJpa.findById(first.sessionId()).orElseThrow();
        AuthSessionEntity replacement = authJpa.findById(second.sessionId()).orElseThrow();
        assertThat(old.getRevokedAt()).isNotNull();
        assertThat(old.getRevokeReason()).isEqualTo("ROTATED");
        assertThat(old.getReplacedBySessionId()).isEqualTo(second.sessionId());
        assertThat(replacement.getTokenFamilyId()).isEqualTo(old.getTokenFamilyId());
        assertThat(second.expiresAt()).isEqualTo(first.expiresAt());
        assertThat(old.getRefreshTokenHash()).isEqualTo(codec.hash(first.token())).doesNotContain(first.token());
        assertThat(replacement.getRefreshTokenHash()).isEqualTo(codec.hash(second.token()));
    }

    @Test
    @DisplayName("ใช้ refresh token เก่าซ้ำแล้ว revoke replacement ทั้ง family")
    void reuseRevokesTheWholeTokenFamily() {
        RefreshTokenResult first = service.create(user.getId(), "browser-a");
        entityManager.flush();
        RefreshTokenResult second = service.rotate(first.token(), "browser-a");
        entityManager.flush();

        assertThatThrownBy(() -> service.rotate(first.token(), "browser-a"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).errorCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
        entityManager.flush();

        assertThat(authJpa.findById(second.sessionId()).orElseThrow().getRevokedAt()).isNotNull();
        assertThat(authJpa.findById(second.sessionId()).orElseThrow().getRevokeReason())
                .isEqualTo("REUSE_DETECTED");
    }

    @Test
    @DisplayName("client fingerprint เปลี่ยนแล้วปฏิเสธและ revoke token family")
    void clientFingerprintMismatchRevokesFamily() {
        RefreshTokenResult token = service.create(user.getId(), "browser-a");
        entityManager.flush();

        assertThatThrownBy(() -> service.rotate(token.token(), "browser-b"))
                .isInstanceOf(ApiException.class);
        entityManager.flush();

        assertThat(authJpa.findById(token.sessionId()).orElseThrow().getRevokeReason())
                .isEqualTo("CLIENT_MISMATCH");
    }
}
