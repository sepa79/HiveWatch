package io.pockethive.hivewatch.service.api;

import java.util.List;
import java.util.UUID;

public record ProvisioningPlanApplyResultDto(
        UUID environmentId,
        ProvisioningApplySummaryDto summary,
        List<ProvisioningAppliedObjectDto> objects,
        ProvisioningPlanValidationResultDto validation
) {
}
