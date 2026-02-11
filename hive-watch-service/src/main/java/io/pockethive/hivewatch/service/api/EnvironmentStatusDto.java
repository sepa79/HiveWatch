package io.pockethive.hivewatch.service.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EnvironmentStatusDto(
        UUID environmentId,
        String environmentName,
        DecisionVerdict verdict,
        Instant evaluatedAt,
        List<DecisionIssueDto> issues
) {
}

