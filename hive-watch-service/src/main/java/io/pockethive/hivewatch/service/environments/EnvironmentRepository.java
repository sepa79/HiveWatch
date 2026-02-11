package io.pockethive.hivewatch.service.environments;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentRepository extends JpaRepository<EnvironmentEntity, UUID> {
}

