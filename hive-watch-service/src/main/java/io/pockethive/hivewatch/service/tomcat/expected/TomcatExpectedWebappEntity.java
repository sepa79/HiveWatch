package io.pockethive.hivewatch.service.tomcat.expected;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TomcatExpectedWebappEntity() {
    }

    public TomcatExpectedWebappEntity(UUID id, UUID serverId, String role, String path, Instant createdAt) {
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

    public String getRole() {
        return role;
    }

    public String getPath() {
        return path;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
