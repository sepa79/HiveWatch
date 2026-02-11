package io.pockethive.hivewatch.service.api;

import java.util.List;

public record DashboardSectionDto(
        DashboardSectionKind kind,
        String title,
        List<DashboardColumnDto> columns,
        List<DashboardRowDto> rows
) {
}

