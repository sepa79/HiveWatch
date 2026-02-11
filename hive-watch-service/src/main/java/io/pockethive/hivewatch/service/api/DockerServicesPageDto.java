package io.pockethive.hivewatch.service.api;

import java.util.List;

public record DockerServicesPageDto(
        int page,
        int size,
        long total,
        List<DockerServiceListItemDto> items
) {
}

