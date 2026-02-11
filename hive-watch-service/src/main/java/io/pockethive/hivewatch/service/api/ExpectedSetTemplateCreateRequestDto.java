package io.pockethive.hivewatch.service.api;

import java.util.List;

public record ExpectedSetTemplateCreateRequestDto(
        ExpectedSetTemplateKind kind,
        String name,
        List<String> items
) {
}

