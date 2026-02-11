package io.pockethive.hivewatch.service.actuator;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActuatorTargetRepository extends JpaRepository<ActuatorTargetEntity, UUID> {
    List<ActuatorTargetEntity> findByServerIdIn(List<UUID> serverIds);
}

