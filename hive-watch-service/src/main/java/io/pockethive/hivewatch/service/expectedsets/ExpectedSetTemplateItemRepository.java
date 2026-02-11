package io.pockethive.hivewatch.service.expectedsets;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpectedSetTemplateItemRepository extends JpaRepository<ExpectedSetTemplateItemEntity, UUID> {
    List<ExpectedSetTemplateItemEntity> findByTemplateIdIn(List<UUID> templateIds);
}

