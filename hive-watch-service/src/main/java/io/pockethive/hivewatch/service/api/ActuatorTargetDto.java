package io.pockethive.hivewatch.service.api;

import java.util.UUID;

public record ActuatorTargetDto(
        UUID id,
        UUID serverId,
        String serverName,
        TomcatRole role,
        String baseUrl,
        int port,
        String profile,
        int connectTimeoutMs,
        int requestTimeoutMs,
        ActuatorTargetStateDto state
) {
}
