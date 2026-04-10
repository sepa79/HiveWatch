package io.pockethive.hivewatch.service.api;

public record ProvisioningPlanApplyRequestDto(
        EnvironmentProvisioningPlanDto plan,
        boolean scanAfterApply
) {
}
