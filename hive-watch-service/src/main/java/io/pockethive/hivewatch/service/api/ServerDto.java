package io.pockethive.hivewatch.service.api;

import java.util.UUID;

public record ServerDto(UUID id, UUID environmentId, String name) {
}

