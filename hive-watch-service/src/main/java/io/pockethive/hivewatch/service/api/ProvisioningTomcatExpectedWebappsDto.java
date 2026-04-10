package io.pockethive.hivewatch.service.api;

import java.util.List;

public record ProvisioningTomcatExpectedWebappsDto(
        ProvisioningExpectedSetChangeModeDto changeMode,
        List<ProvisioningTomcatExpectedWebappsSpecDto> specs
) {
}
