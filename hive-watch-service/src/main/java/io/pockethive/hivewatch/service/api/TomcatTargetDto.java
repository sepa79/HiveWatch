package io.pockethive.hivewatch.service.api;

import java.util.UUID;

public record TomcatTargetDto(
        UUID id,
        UUID serverId,
        String serverName,
        TomcatRole role,
        String baseUrl,
        int port,
        TomcatTargetStateDto state
) {
}
