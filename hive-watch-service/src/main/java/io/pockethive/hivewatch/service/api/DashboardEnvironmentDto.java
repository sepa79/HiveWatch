package io.pockethive.hivewatch.service.api;

import java.time.Instant;
import java.util.UUID;

public record DashboardEnvironmentDto(
        UUID id,
        String name,
        int tomcatTargets,
        int tomcatOk,
        int tomcatError,
        int tomcatWebappsTotal,
        Instant tomcatLastScanAt,
        TomcatEnvironmentStatus tomcatStatus,
        int actuatorTargets,
        int actuatorUp,
        int actuatorDown,
        int actuatorError,
        Instant actuatorLastScanAt,
        TomcatEnvironmentStatus actuatorStatus
) {
}
