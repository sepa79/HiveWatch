package io.pockethive.hivewatch.service.tomcat.expected;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TomcatExpectedWebappRepository extends JpaRepository<TomcatExpectedWebappEntity, UUID> {
    List<TomcatExpectedWebappEntity> findByServerIdIn(List<UUID> serverIds);

    void deleteByServerIdIn(List<UUID> serverIds);
}
