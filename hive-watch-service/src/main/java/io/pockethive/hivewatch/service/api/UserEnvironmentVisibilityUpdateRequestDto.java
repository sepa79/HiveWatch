package io.pockethive.hivewatch.service.api;

import java.util.List;
import java.util.UUID;

public record UserEnvironmentVisibilityUpdateRequestDto(
        List<UUID> environmentIds
) {
}

