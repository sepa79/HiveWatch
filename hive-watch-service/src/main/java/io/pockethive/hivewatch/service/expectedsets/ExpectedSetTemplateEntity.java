package io.pockethive.hivewatch.service.expectedsets;

import io.pockethive.hivewatch.service.api.ExpectedSetTemplateKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hw_expected_set_templates")
public class ExpectedSetTemplateEntity {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private ExpectedSetTemplateKind kind;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ExpectedSetTemplateEntity() {
    }

    public ExpectedSetTemplateEntity(UUID id, ExpectedSetTemplateKind kind, String name, Instant createdAt) {
        this.id = id;
        this.kind = kind;
        this.name = name;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public ExpectedSetTemplateKind getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

