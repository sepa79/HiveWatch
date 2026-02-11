package io.pockethive.hivewatch.service.api;

import java.util.List;
import java.util.UUID;

public record DashboardRowDto(
        UUID id,
        String label,
        String link,
        List<DashboardCellDto> cells,
        DashboardRowStatus status
) {
}

