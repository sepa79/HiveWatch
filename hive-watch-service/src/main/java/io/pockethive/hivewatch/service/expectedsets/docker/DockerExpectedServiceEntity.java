package io.pockethive.hivewatch.service.expectedsets.docker;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hw_docker_expected_services")
public class DockerExpectedServiceEntity {
    @Id
    private UUID id;

    @Column(name = "server_id", nullable = false)
    private UUID serverId;

    @Column(name = "profile", nullable = false)
    private String profile;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DockerExpectedServiceEntity() {
    }

    public DockerExpectedServiceEntity(UUID id, UUID serverId, String profile, Instant createdAt) {
        this.id = id;
        this.serverId = serverId;
        this.profile = profile;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getServerId() {
        return serverId;
    }

    public String getProfile() {
        return profile;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

