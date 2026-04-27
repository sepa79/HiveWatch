package io.pockethive.hivewatch.service.expectedsets.tomcat;

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
@Table(name = "hw_tomcat_expected_webapp_specs")
public class TomcatExpectedWebappSpecEntity {
    @Id
    private UUID id;

    @Column(name = "server_id", nullable = false)
    private UUID serverId;

    @Column(name = "role", nullable = false)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false)
    private ExpectedSetMode mode;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TomcatExpectedWebappSpecEntity() {
    }

    public TomcatExpectedWebappSpecEntity(
            UUID id,
            UUID serverId,
            String role,
            ExpectedSetMode mode,
            UUID templateId,
            Instant createdAt
    ) {
        this.id = id;
        this.serverId = serverId;
        this.role = role;
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

    public String getRole() {
        return role;
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
