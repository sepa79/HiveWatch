package io.pockethive.hivewatch.service.expectedsets;

import io.pockethive.hivewatch.service.api.ExpectedSetTemplateKind;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpectedSetTemplateRepository extends JpaRepository<ExpectedSetTemplateEntity, UUID> {
    List<ExpectedSetTemplateEntity> findByKindOrderByNameAsc(ExpectedSetTemplateKind kind);
}

