package io.pockethive.hivewatch.service.api;

import java.time.Instant;
import java.util.UUID;

public record TomcatExpectedWebappDto(
        UUID id,
        UUID serverId,
        TomcatRole role,
        String path,
        Instant createdAt
) {
}

