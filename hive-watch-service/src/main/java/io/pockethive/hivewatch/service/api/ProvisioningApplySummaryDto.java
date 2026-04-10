package io.pockethive.hivewatch.service.api;

public record ProvisioningApplySummaryDto(
        int environmentsCreated,
        int serversCreated,
        int tomcatTargetsCreated,
        int actuatorTargetsCreated,
        int tomcatExpectedWebappSpecsApplied,
        int tomcatExpectedWebappItemsApplied,
        int dockerExpectedServiceSpecsApplied,
        int dockerExpectedServiceItemsApplied
) {
}
