package io.pockethive.hivewatch.service.expectedsets;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hw_expected_set_template_items")
public class ExpectedSetTemplateItemEntity {
    @Id
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "value", nullable = false)
    private String value;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ExpectedSetTemplateItemEntity() {
    }

    public ExpectedSetTemplateItemEntity(UUID id, UUID templateId, String value, Instant createdAt) {
        this.id = id;
        this.templateId = templateId;
        this.value = value;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getValue() {
        return value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

