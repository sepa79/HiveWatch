package io.pockethive.hivewatch.service.api;

import java.util.UUID;

public record DecisionIssueDto(
        DecisionVerdict severity,
        DecisionIssueKind kind,
        UUID targetId,
        String serverName,
        TomcatRole role,
        String label,
        String message
) {
}

