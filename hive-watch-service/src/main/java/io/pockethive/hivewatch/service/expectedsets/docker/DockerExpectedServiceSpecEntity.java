package io.pockethive.hivewatch.service.expectedsets.docker;

import io.pockethive.hivewatch.service.api.ExpectedSetMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hw_docker_expected_service_specs")
public class DockerExpectedServiceSpecEntity {
    @Id
    private UUID id;

    @Column(name = "server_id", nullable = false)
    private UUID serverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false)
    private ExpectedSetMode mode;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DockerExpectedServiceSpecEntity() {
    }

    public DockerExpectedServiceSpecEntity(UUID id, UUID serverId, ExpectedSetMode mode, UUID templateId, Instant createdAt) {
        this.id = id;
        this.serverId = serverId;
        this.mode = mode;
        this.templateId = templateId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getServerId() {
        return serverId;
    }

    public ExpectedSetMode getMode() {
        return mode;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

