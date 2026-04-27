package io.pockethive.hivewatch.service.api;

import java.util.UUID;

public record TomcatTargetDto(
        UUID id,
        UUID serverId,
        String serverName,
        String role,
        String baseUrl,
        int port,
        String username,
        int connectTimeoutMs,
        int requestTimeoutMs,
        TomcatTargetStateDto state
) {
}
