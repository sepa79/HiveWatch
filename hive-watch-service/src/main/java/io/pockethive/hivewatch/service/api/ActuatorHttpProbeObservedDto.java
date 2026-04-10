package io.pockethive.hivewatch.service.api;

public record ActuatorHttpProbeObservedDto(
        TargetAdapterTypeDto adapterType,
        String healthStatus,
        String appName,
        String buildVersion,
        Double cpuUsage,
        Long memoryUsedBytes
) implements TargetProbeObservedDto {
}
