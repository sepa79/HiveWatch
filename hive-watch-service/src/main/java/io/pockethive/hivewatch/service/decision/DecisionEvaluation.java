package io.pockethive.hivewatch.service.decision;

import io.pockethive.hivewatch.service.api.DecisionIssueDto;
import io.pockethive.hivewatch.service.api.DecisionVerdict;
import java.util.List;

public record DecisionEvaluation(
        DecisionVerdict verdict,
        List<DecisionIssueDto> issues,
        int blockIssues,
        int warnIssues,
        int unknownIssues
) {
}

