package io.pockethive.hivewatch.service.api;

import java.util.List;
import java.util.UUID;

public record DashboardEnvironmentBlockDto(
        UUID id,
        String name,
        DashboardEnvironmentSummaryDto summary,
        List<DashboardSectionDto> sections
) {
}

