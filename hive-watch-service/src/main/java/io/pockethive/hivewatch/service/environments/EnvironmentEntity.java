package io.pockethive.hivewatch.service.environments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "hw_environments")
public class EnvironmentEntity {
    @Id
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    protected EnvironmentEntity() {
    }

    public EnvironmentEntity(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

