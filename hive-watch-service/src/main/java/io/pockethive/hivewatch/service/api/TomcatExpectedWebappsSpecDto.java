package io.pockethive.hivewatch.service.api;

import java.util.List;
import java.util.UUID;

public record TomcatExpectedWebappsSpecDto(
        UUID serverId,
        TomcatRole role,
        ExpectedSetMode mode,
        UUID templateId,
        List<String> items
) {
}

