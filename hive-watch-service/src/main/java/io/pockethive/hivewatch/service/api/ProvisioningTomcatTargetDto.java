package io.pockethive.hivewatch.service.api;

public record ProvisioningTomcatTargetDto(
        String role,
        TargetAdapterTypeDto adapterType,
        String baseUrl,
        int port,
        String username,
        String password,
        int connectTimeoutMs,
        int requestTimeoutMs
) {
}
