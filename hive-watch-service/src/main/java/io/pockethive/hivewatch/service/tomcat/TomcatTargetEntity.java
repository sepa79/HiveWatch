package io.pockethive.hivewatch.service.tomcat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hw_tomcat_targets")
public class TomcatTargetEntity {
    @Id
    private UUID id;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(name = "name", nullable = false)
    private String name;

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
            UUID environmentId,
            String name,
            String baseUrl,
            int port,
            String username,
            String password,
            int connectTimeoutMs,
            int requestTimeoutMs,
            Instant createdAt
    ) {
        this.id = id;
        this.environmentId = environmentId;
        this.name = name;
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

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public String getName() {
        return name;
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

