package io.pockethive.hivewatch.service.api;

import java.util.List;
import java.util.UUID;

public record DockerExpectedServicesSpecDto(
        UUID serverId,
        ExpectedSetMode mode,
        UUID templateId,
        List<String> items
) {
}

