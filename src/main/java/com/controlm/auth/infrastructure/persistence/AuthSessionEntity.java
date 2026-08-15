package com.controlm.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * JPA mapping for the {@code auth_session} table defined in Flyway V1: one opaque, rotating
 * refresh session. Only the hash of the refresh token is stored, never the token itself.
 *
 * <p>{@code user_id} and {@code replaced_by_session_id} are held as raw {@link UUID} values so the
 * auth module does not depend on the iam entity graph.
 */
@Entity
@Table(name = "auth_session")
public class AuthSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 255)
    private String refreshTokenHash;

    @Column(name = "token_family_id", nullable = false)
    private UUID tokenFamilyId;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoke_reason", length = 255)
    private String revokeReason;

    @Column(name = "client_fingerprint", length = 255)
    private String clientFingerprint;

    @Column(name = "replaced_by_session_id")
    private UUID replacedBySessionId;

    protected AuthSessionEntity() {
    }

    public AuthSessionEntity(UUID userId, String refreshTokenHash, UUID tokenFamilyId, Instant expiresAt) {
        this.userId = userId;
        this.refreshTokenHash = refreshTokenHash;
        this.tokenFamilyId = tokenFamilyId;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public UUID getTokenFamilyId() {
        return tokenFamilyId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevokeReason() {
        return revokeReason;
    }

    public void setRevokeReason(String revokeReason) {
        this.revokeReason = revokeReason;
    }

    public String getClientFingerprint() {
        return clientFingerprint;
    }

    public void setClientFingerprint(String clientFingerprint) {
        this.clientFingerprint = clientFingerprint;
    }

    public UUID getReplacedBySessionId() {
        return replacedBySessionId;
    }

    public void setReplacedBySessionId(UUID replacedBySessionId) {
        this.replacedBySessionId = replacedBySessionId;
    }

    public void rotate(Instant at, UUID replacementId) {
        this.lastUsedAt = at;
        this.revokedAt = at;
        this.revokeReason = "ROTATED";
        this.replacedBySessionId = replacementId;
    }

    public void revoke(Instant at, String reason) {
        if (this.revokedAt == null) {
            this.revokedAt = at;
            this.revokeReason = reason;
        }
    }
}
