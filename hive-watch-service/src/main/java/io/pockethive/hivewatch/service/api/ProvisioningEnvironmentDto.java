package io.pockethive.hivewatch.service.api;

import java.util.UUID;

public record ProvisioningEnvironmentDto(
        ProvisioningChangeModeDto mode,
        UUID environmentId,
        String name
) {
}
