package io.pockethive.hivewatch.service.api;

import java.util.List;

public record ProvisioningDockerExpectedServicesDto(
        ProvisioningExpectedSetChangeModeDto changeMode,
        List<ProvisioningDockerExpectedServicesSpecDto> specs
) {
}
