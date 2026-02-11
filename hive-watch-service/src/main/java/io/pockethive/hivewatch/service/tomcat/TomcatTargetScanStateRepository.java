package io.pockethive.hivewatch.service.tomcat;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TomcatTargetScanStateRepository extends JpaRepository<TomcatTargetScanStateEntity, UUID> {
}

