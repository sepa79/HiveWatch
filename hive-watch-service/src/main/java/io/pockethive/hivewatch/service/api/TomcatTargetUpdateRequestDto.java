package io.pockethive.hivewatch.service.api;

import java.util.UUID;

public record TomcatTargetUpdateRequestDto(
        UUID serverId,
        String role,
        String baseUrl,
        int port,
        String username,
        String password,
        int connectTimeoutMs,
        int requestTimeoutMs
) {
}

