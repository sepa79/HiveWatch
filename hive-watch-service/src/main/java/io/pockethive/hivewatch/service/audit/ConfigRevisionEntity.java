package io.pockethive.hivewatch.service.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hw_config_revisions")
public class ConfigRevisionEntity {
    @Id
    private UUID id;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(name = "revision_type", nullable = false, columnDefinition = "text")
    private String revisionType;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Column(name = "actor_username", nullable = false, columnDefinition = "text")
    private String actorUsername;

    @Column(name = "source", nullable = false, columnDefinition = "text")
    private String source;

    @Column(name = "correlation_id", columnDefinition = "text")
    private String correlationId;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "plan_hash", nullable = false, columnDefinition = "text")
    private String planHash;

    @Column(name = "plan_json", nullable = false, columnDefinition = "text")
    private String planJson;

    @Column(name = "summary_json", nullable = false, columnDefinition = "text")
    private String summaryJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ConfigRevisionEntity() {
    }

    public ConfigRevisionEntity(
            UUID id,
            UUID environmentId,
            String revisionType,
            UUID actorUserId,
            String actorUsername,
            String source,
            String correlationId,
            String reason,
            String planHash,
            String planJson,
            String summaryJson,
            Instant createdAt
    ) {
        this.id = id;
        this.environmentId = environmentId;
        this.revisionType = revisionType;
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
        this.source = source;
        this.correlationId = correlationId;
        this.reason = reason;
        this.planHash = planHash;
        this.planJson = planJson;
        this.summaryJson = summaryJson;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public String getRevisionType() {
        return revisionType;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getSource() {
        return source;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getReason() {
        return reason;
    }

    public String getPlanHash() {
        return planHash;
    }

    public String getPlanJson() {
        return planJson;
    }

    public String getSummaryJson() {
        return summaryJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
