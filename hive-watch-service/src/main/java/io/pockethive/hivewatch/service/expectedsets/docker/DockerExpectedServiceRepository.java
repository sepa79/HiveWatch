package io.pockethive.hivewatch.service.expectedsets.docker;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DockerExpectedServiceRepository extends JpaRepository<DockerExpectedServiceEntity, UUID> {
    List<DockerExpectedServiceEntity> findByServerIdIn(List<UUID> serverIds);

    void deleteByServerId(UUID serverId);

    void deleteByServerIdIn(List<UUID> serverIds);
}
