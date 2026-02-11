package io.pockethive.hivewatch.service.tomcat.expected;

import io.pockethive.hivewatch.service.api.TomcatRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hw_tomcat_expected_webapps")
public class TomcatExpectedWebappEntity {
    @Id
    private UUID id;

    @Column(name = "server_id", nullable = false)
    private UUID serverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private TomcatRole role;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TomcatExpectedWebappEntity() {
    }

    public TomcatExpectedWebappEntity(UUID id, UUID serverId, TomcatRole role, String path, Instant createdAt) {
        this.id = id;
        this.serverId = serverId;
        this.role = role;
        this.path = path;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getServerId() {
        return serverId;
    }

    public TomcatRole getRole() {
        return role;
    }

    public String getPath() {
        return path;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

