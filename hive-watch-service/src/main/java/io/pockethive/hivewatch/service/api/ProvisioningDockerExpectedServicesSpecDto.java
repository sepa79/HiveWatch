package io.pockethive.hivewatch.service.api;

import java.util.List;
import java.util.UUID;

public record ProvisioningDockerExpectedServicesSpecDto(
        ExpectedSetMode mode,
        UUID templateId,
        List<String> items
) {
}
