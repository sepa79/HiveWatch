package io.pockethive.hivewatch.service.api;

import java.util.List;

public record EnvironmentProvisioningPlanDto(
        String source,
        String correlationId,
        String reason,
        ProvisioningEnvironmentDto environment,
        List<ProvisioningServerDto> servers
) {
}
