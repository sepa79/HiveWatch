package io.pockethive.hivewatch.service.actuator;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ActuatorTargetRepository extends JpaRepository<ActuatorTargetEntity, UUID> {
    List<ActuatorTargetEntity> findByServerIdIn(List<UUID> serverIds);

    Page<ActuatorTargetEntity> findByServerId(UUID serverId, Pageable pageable);

    Page<ActuatorTargetEntity> findByServerIdAndProfileContainingIgnoreCase(UUID serverId, String profile, Pageable pageable);
}
