package io.pockethive.hivewatch.service.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hw_audit_events")
public class AuditEventEntity {
    @Id
    private UUID id;

    @Column(name = "revision_id")
    private UUID revisionId;

    @Column(name = "environment_id")
    private UUID environmentId;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Column(name = "actor_username", nullable = false, columnDefinition = "text")
    private String actorUsername;

    @Column(name = "action", nullable = false, columnDefinition = "text")
    private String action;

    @Column(name = "object_type", nullable = false, columnDefinition = "text")
    private String objectType;

    @Column(name = "object_id")
    private UUID objectId;

    @Column(name = "object_label", nullable = false, columnDefinition = "text")
    private String objectLabel;

    @Column(name = "source", nullable = false, columnDefinition = "text")
    private String source;

    @Column(name = "correlation_id", columnDefinition = "text")
    private String correlationId;

    @Column(name = "details_json", nullable = false, columnDefinition = "text")
    private String detailsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEventEntity() {
    }

    public AuditEventEntity(
            UUID id,
            UUID revisionId,
            UUID environmentId,
            UUID actorUserId,
            String actorUsername,
            String action,
            String objectType,
            UUID objectId,
            String objectLabel,
            String source,
            String correlationId,
            String detailsJson,
            Instant createdAt
    ) {
        this.id = id;
        this.revisionId = revisionId;
        this.environmentId = environmentId;
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
        this.action = action;
        this.objectType = objectType;
        this.objectId = objectId;
        this.objectLabel = objectLabel;
        this.source = source;
        this.correlationId = correlationId;
        this.detailsJson = detailsJson;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRevisionId() {
        return revisionId;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getAction() {
        return action;
    }

    public String getObjectType() {
        return objectType;
    }

    public UUID getObjectId() {
        return objectId;
    }

    public String getObjectLabel() {
        return objectLabel;
    }

    public String getSource() {
        return source;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
