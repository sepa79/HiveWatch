package io.pockethive.hivewatch.service.api;

import java.util.List;

public record EnvironmentTargetRoleReplaceRequestDto(
        List<EnvironmentTargetRoleDto> roles
) {
}
