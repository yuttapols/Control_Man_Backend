package com.controlm.iam.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** JPA mapping for the {@code role_permission} grant table defined in Flyway V1. */
@Entity
@Table(name = "role_permission")
public class RolePermissionEntity {

    @EmbeddedId
    private RolePermissionId id;

    @CreationTimestamp
    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "granted_by")
    private UUID grantedBy;

    protected RolePermissionEntity() {
    }

    public RolePermissionEntity(UUID roleId, UUID permissionId) {
        this.id = new RolePermissionId(roleId, permissionId);
    }

    public RolePermissionId getId() {
        return id;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(UUID grantedBy) {
        this.grantedBy = grantedBy;
    }
}
