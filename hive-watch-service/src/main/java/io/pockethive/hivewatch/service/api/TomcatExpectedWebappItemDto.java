package io.pockethive.hivewatch.service.api;

import java.util.UUID;

public record TomcatExpectedWebappItemDto(
        UUID serverId,
        TomcatRole role,
        String path
) {
}

