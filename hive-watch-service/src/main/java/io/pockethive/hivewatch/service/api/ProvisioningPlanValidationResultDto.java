package io.pockethive.hivewatch.service.api;

import java.util.List;

public record ProvisioningPlanValidationResultDto(
        boolean valid,
        List<ProvisioningPlanIssueDto> errors,
        List<ProvisioningPlanIssueDto> warnings,
        List<ProvisioningPlanDiffDto> diff
) {
}
