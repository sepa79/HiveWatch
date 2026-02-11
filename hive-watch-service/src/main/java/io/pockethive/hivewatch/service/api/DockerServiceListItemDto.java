package io.pockethive.hivewatch.service.api;

import java.time.Instant;
import java.util.UUID;

public record DockerServiceListItemDto(
        UUID targetId,
        String profile,
        String appName,
        String buildVersion,
        TomcatScanOutcomeKind outcomeKind,
        TomcatScanErrorKind errorKind,
        String errorMessage,
        String healthStatus,
        Instant scannedAt
) {
}
