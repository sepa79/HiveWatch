package io.pockethive.hivewatch.service.audit;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigRevisionRepository extends JpaRepository<ConfigRevisionEntity, UUID> {
}
