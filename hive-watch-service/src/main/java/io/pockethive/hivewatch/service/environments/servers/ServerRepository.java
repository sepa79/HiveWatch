package io.pockethive.hivewatch.service.environments.servers;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerRepository extends JpaRepository<ServerEntity, UUID> {
    List<ServerEntity> findByEnvironmentId(UUID environmentId);

    List<ServerEntity> findByEnvironmentIdIn(List<UUID> environmentIds);

    boolean existsByIdAndEnvironmentId(UUID id, UUID environmentId);

    boolean existsByEnvironmentIdAndName(UUID environmentId, String name);
}
