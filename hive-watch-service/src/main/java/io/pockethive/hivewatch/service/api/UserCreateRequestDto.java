package io.pockethive.hivewatch.service.api;

import java.util.List;

public record UserCreateRequestDto(
        String username,
        String displayName,
        List<HiveWatchRole> roles,
        boolean active
) {
}

