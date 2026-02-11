package io.pockethive.hivewatch.service.api;

import java.util.UUID;

public record EnvironmentSummaryDto(
    UUID id,
    String name
) {
}

