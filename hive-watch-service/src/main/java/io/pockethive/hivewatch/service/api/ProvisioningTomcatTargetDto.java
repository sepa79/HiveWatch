package io.pockethive.hivewatch.service.api;

public record ProvisioningTomcatTargetDto(
        TomcatRole role,
        TargetAdapterTypeDto adapterType,
        String baseUrl,
        int port,
        String username,
        String password,
        int connectTimeoutMs,
        int requestTimeoutMs
) {
}
