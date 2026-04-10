package io.pockethive.hivewatch.service.api;

public record ProvisioningPlanDiffDto(
        ProvisioningPlanDiffActionDto action,
        ProvisioningPlanObjectTypeDto objectType,
        String clientRef,
        String label
) {
}
