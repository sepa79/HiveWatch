package io.pockethive.hivewatch.service.api;

import java.util.List;

public record DashboardDto(
        List<DashboardEnvironmentBlockDto> environments
) {
}

