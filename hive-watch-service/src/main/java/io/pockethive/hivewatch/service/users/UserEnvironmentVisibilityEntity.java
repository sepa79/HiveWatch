package io.pockethive.hivewatch.service.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hw_user_environment_visibility")
public class UserEnvironmentVisibilityEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserEnvironmentVisibilityEntity() {
    }

    public UserEnvironmentVisibilityEntity(UUID id, UUID userId, UUID environmentId, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.environmentId = environmentId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

