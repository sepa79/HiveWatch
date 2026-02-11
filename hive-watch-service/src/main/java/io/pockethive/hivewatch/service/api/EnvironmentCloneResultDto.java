package io.pockethive.hivewatch.service.api;

public record EnvironmentCloneResultDto(
        int servers,
        int tomcatTargets,
        int actuatorTargets,
        int tomcatExpectedSpecs,
        int tomcatExpectedItems,
        int dockerExpectedSpecs,
        int dockerExpectedItems
) {
}

