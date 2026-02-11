package io.pockethive.hivewatch.service.users;

import io.pockethive.hivewatch.service.api.HiveWatchRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hw_user_roles")
public class UserRoleEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private HiveWatchRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserRoleEntity() {
    }

    public UserRoleEntity(UUID id, UUID userId, HiveWatchRole role, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public HiveWatchRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

