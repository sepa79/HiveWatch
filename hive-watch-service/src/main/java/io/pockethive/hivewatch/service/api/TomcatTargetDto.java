package io.pockethive.hivewatch.service.api;

import java.util.UUID;

public record TomcatTargetDto(
        UUID id,
        UUID environmentId,
        String name,
        String baseUrl,
        int port,
        TomcatTargetStateDto state
) {
}

