package io.pockethive.hivewatch.service.security;

import io.pockethive.hivewatch.service.api.HiveWatchRole;
import java.util.Set;
import java.util.UUID;

public record HiveWatchPrincipal(
        UUID userId,
        String username,
        String displayName,
        Set<HiveWatchRole> roles
) {
}
