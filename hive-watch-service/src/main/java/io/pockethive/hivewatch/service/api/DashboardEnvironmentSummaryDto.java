package io.pockethive.hivewatch.service.api;

import java.time.Instant;

public record DashboardEnvironmentSummaryDto(
        DashboardGroupSummaryDto tomcats,
        DashboardGroupSummaryDto docker,
        DashboardGroupSummaryDto aws,
        DecisionVerdict verdict,
        int blockIssues,
        int warnIssues,
        int unknownIssues,
        Instant evaluatedAt
) {
}

