package io.pockethive.hivewatch.service.api;

import java.util.List;
import java.util.UUID;

public record ProvisioningServerDto(
        String clientRef,
        ProvisioningChangeModeDto mode,
        UUID serverId,
        String name,
        List<ProvisioningTomcatTargetDto> tomcatTargets,
        List<ProvisioningActuatorTargetDto> actuatorTargets
) {
}
