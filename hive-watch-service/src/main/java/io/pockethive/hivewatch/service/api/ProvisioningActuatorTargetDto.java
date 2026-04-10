package io.pockethive.hivewatch.service.api;

public record ProvisioningActuatorTargetDto(
        TomcatRole role,
        TargetAdapterTypeDto adapterType,
        String baseUrl,
        int port,
        String profile,
        int connectTimeoutMs,
        int requestTimeoutMs
) {
}
