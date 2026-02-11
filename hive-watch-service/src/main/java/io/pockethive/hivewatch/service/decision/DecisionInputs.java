package io.pockethive.hivewatch.service.decision;

import io.pockethive.hivewatch.service.api.TomcatRole;
import io.pockethive.hivewatch.service.api.TomcatScanErrorKind;
import io.pockethive.hivewatch.service.api.TomcatScanOutcomeKind;
import java.time.Instant;
import java.util.UUID;

public final class DecisionInputs {
    private DecisionInputs() {
    }

    public record TomcatTargetObservation(
            UUID targetId,
            String serverName,
            TomcatRole role,
            String baseUrl,
            int port,
            Instant scannedAt,
            TomcatScanOutcomeKind outcomeKind,
            TomcatScanErrorKind errorKind,
            String errorMessage
    ) {
    }

    public record ActuatorTargetObservation(
            UUID targetId,
            String serverName,
            TomcatRole role,
            String baseUrl,
            int port,
            String profile,
            Instant scannedAt,
            TomcatScanOutcomeKind outcomeKind,
            TomcatScanErrorKind errorKind,
            String errorMessage,
            String healthStatus,
            String appName,
            Double cpuUsage,
            Long memoryUsedBytes
    ) {
    }
}

