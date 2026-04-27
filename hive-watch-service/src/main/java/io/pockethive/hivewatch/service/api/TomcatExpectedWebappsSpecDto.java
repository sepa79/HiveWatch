package io.pockethive.hivewatch.service.api;

import java.util.List;
import java.util.UUID;

public record TomcatExpectedWebappsSpecDto(
        UUID serverId,
        String role,
        ExpectedSetMode mode,
        UUID templateId,
        List<String> items
) {
}

