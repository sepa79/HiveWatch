package io.pockethive.hivewatch.service.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExpectedSetTemplateDto(
        UUID id,
        ExpectedSetTemplateKind kind,
        String name,
        List<String> items,
        Instant createdAt
) {
}

