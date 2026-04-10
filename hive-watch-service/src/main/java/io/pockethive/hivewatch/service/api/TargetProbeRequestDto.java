package io.pockethive.hivewatch.service.api;

public record TargetProbeRequestDto(
        TargetAdapterTypeDto adapterType,
        String baseUrl,
        int port,
        String username,
        String password,
        String profile,
        int connectTimeoutMs,
        int requestTimeoutMs
) {
}
