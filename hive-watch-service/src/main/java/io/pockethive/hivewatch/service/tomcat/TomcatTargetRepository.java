package io.pockethive.hivewatch.service.tomcat;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TomcatTargetRepository extends JpaRepository<TomcatTargetEntity, UUID> {
    List<TomcatTargetEntity> findByServerIdIn(List<UUID> serverIds);
}
