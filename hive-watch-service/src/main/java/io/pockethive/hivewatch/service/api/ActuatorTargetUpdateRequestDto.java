package io.pockethive.hivewatch.service.api;

import java.util.UUID;

public record ActuatorTargetUpdateRequestDto(
        UUID serverId,
        String role,
        String baseUrl,
        int port,
        String profile,
        int connectTimeoutMs,
        int requestTimeoutMs
) {
}

