package io.pockethive.hivewatch.service.expectedsets.tomcat;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TomcatExpectedWebappSpecRepository extends JpaRepository<TomcatExpectedWebappSpecEntity, UUID> {
    List<TomcatExpectedWebappSpecEntity> findByServerIdIn(List<UUID> serverIds);

    void deleteByServerIdIn(List<UUID> serverIds);
}

