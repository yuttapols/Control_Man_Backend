package com.controlm.auth.application;

import com.controlm.auth.application.port.AuthSessionRepository;
import com.controlm.auth.infrastructure.persistence.AuthSessionEntity;
import com.controlm.auth.infrastructure.token.RefreshTokenCodec;
import com.controlm.auth.infrastructure.token.RefreshTokenProperties;
import com.controlm.shared.error.ApiException;
import com.controlm.shared.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    static final String ROTATED = "ROTATED";
    static final String REUSE_DETECTED = "REUSE_DETECTED";

    private final AuthSessionRepository sessions;
    private final RefreshTokenCodec codec;
    private final RefreshTokenProperties properties;
    private final Clock clock;

    @Autowired
    public RefreshTokenServiceImpl(AuthSessionRepository sessions, RefreshTokenCodec codec,
            RefreshTokenProperties properties) {
        this(sessions, codec, properties, Clock.systemUTC());
    }

    RefreshTokenServiceImpl(AuthSessionRepository sessions, RefreshTokenCodec codec,
            RefreshTokenProperties properties, Clock clock) {
        this.sessions = sessions;
        this.codec = codec;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RefreshTokenResult create(UUID userId, String clientFingerprint) {
        Instant now = clock.instant();
        String raw = codec.generate();
        AuthSessionEntity session = new AuthSessionEntity(
                userId, codec.hash(raw), UUID.randomUUID(), now.plus(properties.refreshTokenTtl()));
        session.setClientFingerprint(clientFingerprint);
        sessions.save(session);
        return new RefreshTokenResult(raw, session.getId(), session.getUserId(), session.getExpiresAt());
    }

    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public RefreshTokenResult rotate(String presentedToken, String clientFingerprint) {
        Instant now = clock.instant();
        AuthSessionEntity current = sessions.findByRefreshTokenHashForUpdate(codec.hash(presentedToken))
                .orElseThrow(RefreshTokenServiceImpl::unauthenticated);

        if (current.getRevokedAt() != null) {
            revokeFamily(current.getTokenFamilyId(), now, REUSE_DETECTED);
            throw unauthenticated();
        }
        Instant lastActivity = current.getLastUsedAt() == null ? current.getIssuedAt() : current.getLastUsedAt();
        if (!current.getExpiresAt().isAfter(now)
                || lastActivity == null
                || !lastActivity.plus(properties.refreshIdleTimeout()).isAfter(now)) {
            current.revoke(now, "EXPIRED");
            throw unauthenticated();
        }
        if (current.getClientFingerprint() != null
                && !current.getClientFingerprint().equals(clientFingerprint)) {
            revokeFamily(current.getTokenFamilyId(), now, "CLIENT_MISMATCH");
            throw unauthenticated();
        }

        String raw = codec.generate();
        AuthSessionEntity replacement = new AuthSessionEntity(
                current.getUserId(), codec.hash(raw), current.getTokenFamilyId(), current.getExpiresAt());
        replacement.setClientFingerprint(clientFingerprint);
        sessions.save(replacement);
        current.rotate(now, replacement.getId());
        return new RefreshTokenResult(raw, replacement.getId(), replacement.getUserId(), replacement.getExpiresAt());
    }

    @Override
    @Transactional
    public void revoke(String presentedToken, String reason) {
        sessions.findByRefreshTokenHashForUpdate(codec.hash(presentedToken))
                .filter(session -> session.getRevokedAt() == null)
                .ifPresent(session -> session.revoke(clock.instant(), safeReason(reason)));
    }

    private void revokeFamily(UUID familyId, Instant now, String reason) {
        sessions.findFamilyForUpdate(familyId).stream()
                .filter(session -> session.getRevokedAt() == null)
                .forEach(session -> session.revoke(now, reason));
    }

    private static String safeReason(String reason) {
        return reason == null || reason.isBlank() ? "REVOKED" : reason.substring(0, Math.min(255, reason.length()));
    }

    private static ApiException unauthenticated() {
        return new ApiException(ErrorCode.UNAUTHENTICATED, "Invalid refresh session");
    }
}
