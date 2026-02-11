package io.pockethive.hivewatch.service.api;

import java.time.Instant;

public record ActuatorTargetStateDto(
        Instant scannedAt,
        TomcatScanOutcomeKind outcomeKind,
        TomcatScanErrorKind errorKind,
        String errorMessage,
        String healthStatus,
        String appName,
        String buildVersion,
        Double cpuUsage,
        Long memoryUsedBytes
) {
}
