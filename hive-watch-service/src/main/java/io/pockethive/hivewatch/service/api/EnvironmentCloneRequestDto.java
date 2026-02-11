package io.pockethive.hivewatch.service.api;

import java.util.UUID;

public record EnvironmentCloneRequestDto(
        UUID sourceEnvironmentId
) {
}

