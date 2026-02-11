package io.pockethive.hivewatch.service.expectedsets;

import io.pockethive.hivewatch.service.api.ExpectedSetTemplateDto;
import io.pockethive.hivewatch.service.api.ExpectedSetTemplateKind;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpectedSetTemplateQueryService {
    private final ExpectedSetTemplateRepository templateRepository;
    private final ExpectedSetTemplateItemRepository itemRepository;

    public ExpectedSetTemplateQueryService(
            ExpectedSetTemplateRepository templateRepository,
            ExpectedSetTemplateItemRepository itemRepository
    ) {
        this.templateRepository = templateRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public List<ExpectedSetTemplateDto> list(ExpectedSetTemplateKind kind) {
        List<ExpectedSetTemplateEntity> templates = templateRepository.findByKindOrderByNameAsc(kind);
        if (templates.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = templates.stream().map(ExpectedSetTemplateEntity::getId).toList();
        Map<UUID, List<String>> itemsByTemplateId = itemRepository.findByTemplateIdIn(ids).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ExpectedSetTemplateItemEntity::getTemplateId,
                        java.util.stream.Collectors.mapping(ExpectedSetTemplateItemEntity::getValue, java.util.stream.Collectors.toList())
                ));

        return templates.stream().map(t -> new ExpectedSetTemplateDto(
                t.getId(),
                t.getKind(),
                t.getName(),
                List.copyOf(itemsByTemplateId.getOrDefault(t.getId(), List.of())),
                t.getCreatedAt()
        )).toList();
    }
}

