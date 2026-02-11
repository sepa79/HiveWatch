package io.pockethive.hivewatch.service.api;

public record DashboardCellDto(
        DashboardCellKind kind,
        String text,
        String title
) {
}

