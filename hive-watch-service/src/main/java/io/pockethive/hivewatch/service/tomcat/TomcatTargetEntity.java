package io.pockethive.hivewatch.service.tomcat;

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
@Table(name = "hw_tomcat_targets")
public class TomcatTargetEntity {
    @Id
    private UUID id;

    @Column(name = "server_id", nullable = false)
    private UUID serverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private TomcatRole role;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "port", nullable = false)
    private int port;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "connect_timeout_ms", nullable = false)
    private int connectTimeoutMs;

    @Column(name = "request_timeout_ms", nullable = false)
    private int requestTimeoutMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TomcatTargetEntity() {
    }

    public TomcatTargetEntity(
            UUID id,
            UUID serverId,
            TomcatRole role,
            String baseUrl,
            int port,
            String username,
            String password,
            int connectTimeoutMs,
            int requestTimeoutMs,
            Instant createdAt
    ) {
        this.id = id;
        this.serverId = serverId;
        this.role = role;
        this.baseUrl = baseUrl;
        this.port = port;
        this.username = username;
        this.password = password;
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

    public TomcatRole getRole() {
        return role;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
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
