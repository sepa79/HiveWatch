package io.pockethive.hivewatch.service.actuator;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActuatorTargetScanStateRepository extends JpaRepository<ActuatorTargetScanStateEntity, UUID> {
}

