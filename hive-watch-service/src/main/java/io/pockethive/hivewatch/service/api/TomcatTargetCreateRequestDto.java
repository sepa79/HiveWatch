package io.pockethive.hivewatch.service.api;

public record TomcatTargetCreateRequestDto(
        String name,
        String baseUrl,
        int port,
        String username,
        String password,
        int connectTimeoutMs,
        int requestTimeoutMs
) {
}

