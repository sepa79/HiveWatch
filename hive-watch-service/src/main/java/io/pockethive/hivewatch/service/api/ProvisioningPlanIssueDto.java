package io.pockethive.hivewatch.service.api;

public record ProvisioningPlanIssueDto(
        ProvisioningPlanIssueSeverityDto severity,
        String path,
        String message
) {
}
