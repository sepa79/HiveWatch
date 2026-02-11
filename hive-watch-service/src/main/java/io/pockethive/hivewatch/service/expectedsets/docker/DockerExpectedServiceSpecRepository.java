package io.pockethive.hivewatch.service.expectedsets.docker;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DockerExpectedServiceSpecRepository extends JpaRepository<DockerExpectedServiceSpecEntity, UUID> {
    List<DockerExpectedServiceSpecEntity> findByServerIdIn(List<UUID> serverIds);

    void deleteByServerId(UUID serverId);

    void deleteByServerIdIn(List<UUID> serverIds);
}
