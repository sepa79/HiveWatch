package io.pockethive.hivewatch.service.api;

import java.time.Instant;

public record DashboardGroupSummaryDto(
        DashboardGroupStatus status,
        int targets,
        Instant lastScanAt
) {
}

