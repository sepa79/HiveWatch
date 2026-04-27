package io.pockethive.hivewatch.service.api;

public record EnvironmentTargetRoleDto(
        String code,
        String label,
        int sortOrder,
        boolean active
) {
}
