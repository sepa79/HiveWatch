package io.pockethive.hivewatch.service.api;

import java.util.UUID;

public record ProvisioningAppliedObjectDto(
        ProvisioningPlanObjectTypeDto objectType,
        String clientRef,
        UUID id,
        String label
) {
}
