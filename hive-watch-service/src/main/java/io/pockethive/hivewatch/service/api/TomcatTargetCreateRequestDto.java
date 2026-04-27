package io.pockethive.hivewatch.service.api;

import java.util.UUID;

public record TomcatTargetCreateRequestDto(
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
