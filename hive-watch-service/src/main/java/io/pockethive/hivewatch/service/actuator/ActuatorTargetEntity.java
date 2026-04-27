package io.pockethive.hivewatch.service.actuator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hw_actuator_targets")
public class ActuatorTargetEntity {
    @Id
    private UUID id;

    @Column(name = "server_id", nullable = false)
    private UUID serverId;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "port", nullable = false)
    private int port;

    @Column(name = "profile", nullable = false)
    private String profile;

    @Column(name = "connect_timeout_ms", nullable = false)
    private int connectTimeoutMs;

    @Column(name = "request_timeout_ms", nullable = false)
    private int requestTimeoutMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ActuatorTargetEntity() {
    }

    public ActuatorTargetEntity(
            UUID id,
            UUID serverId,
            String role,
            String baseUrl,
            int port,
            String profile,
            int connectTimeoutMs,
            int requestTimeoutMs,
            Instant createdAt
    ) {
        this.id = id;
        this.serverId = serverId;
        this.role = role;
        this.baseUrl = baseUrl;
        this.port = port;
        this.profile = profile;
        this.connectTimeoutMs = connectTimeoutMs;
        this.requestTimeoutMs = requestTimeoutMs;
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

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getPort() {
        return port;
    }

    public String getProfile() {
        return profile;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
