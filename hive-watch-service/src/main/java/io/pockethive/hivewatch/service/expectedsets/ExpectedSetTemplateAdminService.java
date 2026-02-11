package io.pockethive.hivewatch.service.expectedsets;

import io.pockethive.hivewatch.service.api.ExpectedSetTemplateCreateRequestDto;
import io.pockethive.hivewatch.service.api.ExpectedSetTemplateDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class ExpectedSetTemplateAdminService {
    private final ExpectedSetTemplateRepository templateRepository;
    private final ExpectedSetTemplateItemRepository itemRepository;

    public ExpectedSetTemplateAdminService(
            ExpectedSetTemplateRepository templateRepository,
            ExpectedSetTemplateItemRepository itemRepository
    ) {
        this.templateRepository = templateRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public ExpectedSetTemplateDto create(ExpectedSetTemplateCreateRequestDto request) {
        validateCreate(request);

        Instant now = Instant.now();
        UUID templateId = UUID.randomUUID();
        ExpectedSetTemplateEntity saved = templateRepository.save(new ExpectedSetTemplateEntity(
                templateId,
                request.kind(),
                request.name().trim(),
                now
        ));

        List<String> items = request.items() == null ? List.of() : request.items();
        List<String> normalized = normalizeItems(items);
        itemRepository.saveAll(normalized.stream()
                .map(v -> new ExpectedSetTemplateItemEntity(UUID.randomUUID(), templateId, v, now))
                .toList());

        return new ExpectedSetTemplateDto(saved.getId(), saved.getKind(), saved.getName(), normalized, saved.getCreatedAt());
    }

    private static void validateCreate(ExpectedSetTemplateCreateRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body is required");
        }
        if (request.kind() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "kind is required");
        }
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "name is required");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "items are required");
        }
    }

    private static List<String> normalizeItems(List<String> items) {
        List<String> normalized = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String raw : items) {
            String v = raw == null ? "" : raw.trim();
            if (v.isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, "items cannot contain empty values");
            }
            if (!seen.add(v)) {
                throw new ResponseStatusException(BAD_REQUEST, "Duplicate item: " + v);
            }
            normalized.add(v);
        }
        return List.copyOf(normalized);
    }
}

