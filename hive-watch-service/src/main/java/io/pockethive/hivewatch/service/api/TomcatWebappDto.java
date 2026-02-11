package io.pockethive.hivewatch.service.api;

public record TomcatWebappDto(
        String path,
        String name,
        String version
) {
}

