package io.pockethive.hivewatch.service.api;

import java.util.List;

public record DockerExpectedServicesSpecReplaceRequestDto(
        List<DockerExpectedServicesSpecDto> specs
) {
}

